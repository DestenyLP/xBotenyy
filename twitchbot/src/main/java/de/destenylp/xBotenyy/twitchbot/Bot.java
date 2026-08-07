package de.destenylp.xBotenyy.twitchbot;

import de.destenylp.xBotenyy.common.automod.AutomodSettings;
import de.destenylp.xBotenyy.common.automod.AutomodSettingsFactory;
import de.destenylp.xBotenyy.common.automod.ai.GroqSafeguardClient;
import de.destenylp.xBotenyy.common.config.CommonConfig;
import de.destenylp.xBotenyy.common.core.AbstractBot;
import de.destenylp.xBotenyy.common.core.PrunableResource;
import de.destenylp.xBotenyy.common.discord.DiscordWebhookClient;
import de.destenylp.xBotenyy.common.moderation.AccountLinkRepository;
import de.destenylp.xBotenyy.common.moderation.ModerationCaseRepository;
import de.destenylp.xBotenyy.common.moderation.PendingLinkVerificationRepository;
import de.destenylp.xBotenyy.common.moderation.bridge.BridgeSettings;
import de.destenylp.xBotenyy.common.moderation.bridge.ModerationBridgeClient;
import de.destenylp.xBotenyy.common.moderation.bridge.ModerationBridgeServer;
import de.destenylp.xBotenyy.common.observability.Metrics;
import de.destenylp.xBotenyy.common.persistence.BackupService;
import de.destenylp.xBotenyy.common.persistence.BackupSettings;
import de.destenylp.xBotenyy.common.persistence.sql.Database;
import de.destenylp.xBotenyy.common.twitch.TwitchAppAccessTokenManager;
import de.destenylp.xBotenyy.common.twitch.TwitchUserTokenManager;
import de.destenylp.xBotenyy.twitchbot.automod.TwitchAutomodAdapter;
import de.destenylp.xBotenyy.twitchbot.automod.TwitchModerationApiClient;
import de.destenylp.xBotenyy.twitchbot.broadcast.TwitchBroadcastScheduler;
import de.destenylp.xBotenyy.twitchbot.chat.TwitchChatClient;
import de.destenylp.xBotenyy.twitchbot.commands.TwitchBotServices;
import de.destenylp.xBotenyy.twitchbot.commands.TwitchCommandManager;
import de.destenylp.xBotenyy.twitchbot.commands.impl.*;
import de.destenylp.xBotenyy.twitchbot.config.TwitchBotProperties;
import de.destenylp.xBotenyy.twitchbot.discordlog.TwitchDiscordLogService;
import de.destenylp.xBotenyy.twitchbot.eventlog.TwitchEventLogService;
import de.destenylp.xBotenyy.twitchbot.moderation.TwitchModerationBridgeHandler;
import de.destenylp.xBotenyy.twitchbot.moderation.TwitchModerationSyncTrigger;
import de.destenylp.xBotenyy.twitchbot.moderation.TwitchRoleSyncService;
import de.destenylp.xBotenyy.twitchbot.persistence.*;
import de.destenylp.xBotenyy.twitchbot.poll.TwitchPollManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class Bot extends AbstractBot {
    private static final Logger LOGGER = LoggerFactory.getLogger(Bot.class);
    private final TwitchBotProperties properties;
    private final Instant startedAt = Instant.now();
    private final TwitchPollManager pollManager = new TwitchPollManager();
    private Database database;
    private TwitchChatClient chatClient;
    private TwitchAutomodAdapter automodAdapter;
    private TwitchCommandManager commandManager;
    private TwitchChannelRepository channelRepository;
    private TwitchWatchtimeRepository watchtimeRepository;
    private TwitchEventLogRepository eventLogRepository;
    private TwitchEventLogService eventLogService;
    private TwitchBroadcastRepository broadcastRepository;
    private TwitchBroadcastScheduler broadcastScheduler;
    private TwitchQuoteRepository quoteRepository;
    private DiscordWebhookClient discordWebhookClient;
    private TwitchDiscordLogService discordLogService;
    private ModerationCaseRepository moderationCaseRepository;
    private AccountLinkRepository accountLinkRepository;
    private PendingLinkVerificationRepository pendingLinkVerificationRepository;
    private ModerationBridgeClient moderationBridgeClient;
    private ModerationBridgeServer moderationBridgeServer;
    private TwitchModerationSyncTrigger moderationSyncTrigger;
    private TwitchRoleSyncService roleSyncService;
    private BackupService backupService;
    private String moderatorUserId;
    private Set<String> channels;

    public Bot(CommonConfig config, TwitchBotProperties properties) {
        super(LOGGER, config, "twitchbot-scheduler", 2);
        this.properties = properties;
    }

    public void start() {
        channels = properties.getChatChannels();
        if (channels.isEmpty()) {
            LOGGER.error("No Twitch channels configured (twitch.chat.channels in twitchbot.properties). Shutting down.");
            return;
        }
        database = Database.open(properties.getDatabaseFile(), "db/migrations/twitchbot");
        channelRepository = new TwitchChannelRepository(database);
        watchtimeRepository = new TwitchWatchtimeRepository(database);
        eventLogRepository = new TwitchEventLogRepository(database);
        eventLogService = new TwitchEventLogService(eventLogRepository);
        broadcastRepository = new TwitchBroadcastRepository(database);
        quoteRepository = new TwitchQuoteRepository(database);
        CustomCommandRepository customCommandRepository = new CustomCommandRepository(database);
        channels.forEach(channelRepository::recordJoin);
        discordWebhookClient = new DiscordWebhookClient();
        discordLogService = new TwitchDiscordLogService(discordWebhookClient, properties.getDiscordLogSettings());
        moderationCaseRepository = new ModerationCaseRepository(database);
        accountLinkRepository = new AccountLinkRepository(database);
        pendingLinkVerificationRepository = new PendingLinkVerificationRepository(database);
        moderationBridgeClient = new ModerationBridgeClient();
        moderationSyncTrigger = new TwitchModerationSyncTrigger(accountLinkRepository, moderationBridgeClient,
                properties::getBridgeSettings);
        roleSyncService = new TwitchRoleSyncService(accountLinkRepository, moderationBridgeClient,
                properties::getBridgeSettings);
        Optional<TwitchUserTokenManager> tokenManager = TwitchUserTokenManager.create(
                config.twitchClientId(), config.twitchClientSecret(), config.twitchBotRefreshToken(),
                config.envFilePath(), "TWITCH_BOT_REFRESH_TOKEN");
        java.util.function.Supplier<String> accessTokenSupplier = tokenManager
                .<java.util.function.Supplier<String>>map(manager -> manager::getAccessToken)
                .orElse(config::twitchModeratorAccessToken);
        if (tokenManager.isPresent()) {
            LOGGER.info("Twitch refresh token found, access token will be refreshed automatically.");
            tokenManager.get().refreshNow();
        } else {
            LOGGER.warn("No TWITCH_BOT_REFRESH_TOKEN set - the access token expires every few hours "
                    + "and must then be renewed manually. See the README for automatic setup.");
        }
        TwitchModerationApiClient moderationApiClient = new TwitchModerationApiClient(
                config.twitchClientId(), accessTokenSupplier, Duration.ofSeconds(10),
                properties.getRestActionMaxAttempts(), Duration.ofSeconds(properties.getRestActionBaseDelaySeconds()));
        Optional<TwitchUserTokenManager> broadcasterTokenManager = TwitchUserTokenManager.create(
                config.twitchClientId(), config.twitchClientSecret(), config.twitchBroadcasterRefreshToken(),
                config.envFilePath(), "TWITCH_BROADCASTER_REFRESH_TOKEN");
        java.util.function.Supplier<String> broadcasterAccessTokenSupplier = null;
        if (broadcasterTokenManager.isPresent()) {
            LOGGER.info("Twitch broadcaster refresh token found, channel info commands are available.");
            broadcasterTokenManager.get().refreshNow();
            broadcasterAccessTokenSupplier = broadcasterTokenManager.get()::getAccessToken;
            moderationApiClient.setBroadcasterAccessTokenSupplier(broadcasterAccessTokenSupplier);
        } else if (config.hasTwitchBroadcasterAccessToken()) {
            broadcasterAccessTokenSupplier = config::twitchBroadcasterAccessToken;
            moderationApiClient.setBroadcasterAccessTokenSupplier(broadcasterAccessTokenSupplier);
        } else {
            LOGGER.warn("No TWITCH_BROADCASTER_REFRESH_TOKEN set - !title and !game are disabled. "
                    + "See the README for setting up the broadcaster token.");
        }
        moderatorUserId = moderationApiClient.resolveUserId(config.twitchChatBotUsername())
                .orElseThrow(() -> new IllegalStateException(
                        "Konnte die Twitch-Nutzer-ID des Bot-Accounts nicht aufloesen - "
                                + "TWITCH_BOT_USERNAME und TWITCH_MODERATOR_ACCESS_TOKEN pruefen."));
        BridgeSettings bridgeSettings = properties.getBridgeSettings();
        if (bridgeSettings.isServerEnabled()) {
            TwitchModerationBridgeHandler bridgeHandler = new TwitchModerationBridgeHandler(
                    properties.getModerationSyncChannel(), moderatorUserId, moderationApiClient,
                    moderationCaseRepository, accountLinkRepository, pendingLinkVerificationRepository);
            moderationBridgeServer = new ModerationBridgeServer(bridgeSettings, bridgeHandler);
            moderationBridgeServer.start();
        }
        TwitchAppAccessTokenManager appAccessTokenManager = new TwitchAppAccessTokenManager(
                config.twitchClientId(), config.twitchClientSecret(), Duration.ofSeconds(10), Duration.ofMinutes(10),
                properties.getRestActionMaxAttempts(), Duration.ofSeconds(properties.getRestActionBaseDelaySeconds()));
        chatClient = new TwitchChatClient(config.twitchClientId(), appAccessTokenManager, accessTokenSupplier,
                moderatorUserId, moderationApiClient::resolveUserId, channels, properties.getReconnectDelaySeconds(),
                properties.getMaxReconnectDelaySeconds(), Duration.ofSeconds(10),
                properties.getRestActionMaxAttempts(), Duration.ofSeconds(properties.getRestActionBaseDelaySeconds()));
        de.destenylp.xBotenyy.twitchbot.alerts.TwitchAlertSettings alertSettings =
                new de.destenylp.xBotenyy.twitchbot.alerts.TwitchAlertSettings(
                        properties.isFollowChatAlertEnabled(), properties.getFollowChatAlertMessage(),
                        properties.isFollowDiscordAlertEnabled(),
                        properties.isSubscribeChatAlertEnabled(), properties.getSubscribeChatAlertMessage(),
                        properties.isSubscribeDiscordAlertEnabled(),
                        properties.isRaidChatAlertEnabled(), properties.getRaidChatAlertMessage(),
                        properties.isRaidDiscordAlertEnabled());
        de.destenylp.xBotenyy.twitchbot.alerts.TwitchAlertService alertService =
                new de.destenylp.xBotenyy.twitchbot.alerts.TwitchAlertService(chatClient, discordWebhookClient,
                        properties.getDiscordAlertWebhookUrl(), alertSettings);
        chatClient.setBroadcasterAccessTokenSupplier(broadcasterAccessTokenSupplier);
        chatClient.setAlertSubscriptions(alertSettings.anyFollowAlertEnabled(), alertSettings.anySubscribeAlertEnabled(),
                alertSettings.anyRaidAlertEnabled());
        AutomodSettings automodSettings = AutomodSettingsFactory.from(properties::getRawProperty);
        GroqSafeguardClient moderationClient = automodSettings.getAiFilter().enabled() && config.hasGroqApiKey()
                ? new GroqSafeguardClient(config.groqApiKey(), Duration.ofSeconds(Math.max(automodSettings.getAiFilter().timeoutSeconds(), 1)))
                : null;
        automodAdapter = new TwitchAutomodAdapter(automodSettings, moderationClient, chatClient,
                moderationApiClient, properties.getWarnMessageTemplate(), moderatorUserId, eventLogService,
                discordLogService);
        TwitchBotServices services = new TwitchBotServices(chatClient, automodAdapter.getEngine(), automodAdapter,
                customCommandRepository, watchtimeRepository, moderationApiClient, moderatorUserId, startedAt);
        commandManager = new TwitchCommandManager(properties.getCommandPrefix(), customCommandRepository, services,
                eventLogService, discordLogService);
        registerCommands(commandManager, customCommandRepository);
        broadcastScheduler = new TwitchBroadcastScheduler(broadcastRepository, chatClient, eventLogService, channels);
        broadcastScheduler.start(scheduler, properties.getBroadcastCheckIntervalSeconds());
        chatClient.onMessage(message -> {
            try {
                recordActivityQuietly(message.channelLogin());
                Metrics.increment("twitch.messages_processed");
                discordLogService.logChatMessage(message);
                roleSyncService.handleMessage(message);
                boolean flaggedByAutomod = automodAdapter.handleMessage(message);
                if (!flaggedByAutomod) {
                    commandManager.handleMessage(message);
                }
            } catch (Exception e) {
                LOGGER.error("Unexpected error while processing a Twitch message in channel {}: ",
                        message.channelLogin(), e);
            }
        });
        chatClient.onAutomodHeld(held -> {
            try {
                eventLogService.record(held.channelLogin(), held.userId(), "TWITCH_AUTOMOD_HOLD",
                        "category=" + held.category() + " level=" + held.level());
                discordLogService.logNativeAutomodHold(held.channelLogin(), held.userLogin(), held.content(),
                        held.category(), held.level());
            } catch (Exception e) {
                LOGGER.error("Unexpected error while processing a Twitch AutoMod hold in channel {}: ",
                        held.channelLogin(), e);
            }
        });
        chatClient.onAutomodUpdate(update -> {
            try {
                eventLogService.record(update.channelLogin(), update.userId(), "TWITCH_AUTOMOD_UPDATE",
                        "status=" + update.status());
                discordLogService.logNativeAutomodUpdate(update.channelLogin(), update.userLogin(), update.status(),
                        update.moderatorLogin());
            } catch (Exception e) {
                LOGGER.error("Unexpected error while processing a Twitch AutoMod update in channel {}: ",
                        update.channelLogin(), e);
            }
        });
        chatClient.onFollow(event -> {
            try {
                eventLogService.record(event.channelLogin(), event.userId(), "TWITCH_FOLLOW", "");
                alertService.handleFollow(event);
            } catch (Exception e) {
                LOGGER.error("Unexpected error while processing a Twitch follow in channel {}: ",
                        event.channelLogin(), e);
            }
        });
        chatClient.onSubscribe(event -> {
            try {
                eventLogService.record(event.channelLogin(), event.userId(), "TWITCH_SUBSCRIBE", "tier=" + event.tier());
                alertService.handleSubscribe(event);
            } catch (Exception e) {
                LOGGER.error("Unexpected error while processing a Twitch subscribe in channel {}: ",
                        event.channelLogin(), e);
            }
        });
        chatClient.onRaid(event -> {
            try {
                eventLogService.record(event.channelLogin(), event.fromUserId(), "TWITCH_RAID",
                        "viewers=" + event.viewers());
                alertService.handleRaid(event);
            } catch (Exception e) {
                LOGGER.error("Unexpected error while processing a Twitch raid in channel {}: ",
                        event.channelLogin(), e);
            }
        });
        chatClient.onConnected(() -> LOGGER.info("Twitch bot is active in {} channels: {}", channels.size(), channels));
        startDataRetentionTask();
        startHeartbeat();
        startBackupSchedule();
        startWatchtimeTracking(moderationApiClient);
        startRoleReconciliation(moderationApiClient);
        registerShutdownHook();
        LOGGER.info("Starting Twitch bot as {} for channels: {}", config.twitchChatBotUsername(), channels);
        chatClient.connect();
    }

    private void registerCommands(TwitchCommandManager commandManager, CustomCommandRepository customCommandRepository) {
        commandManager.register(new PingCommand());
        commandManager.register(new UptimeCommand());
        commandManager.register(new StrikesCommand());
        commandManager.register(new AutomodStatusCommand());
        commandManager.register(new CustomCommandManagementCommand(customCommandRepository, eventLogService));
        commandManager.register(new CommandsCommand(commandManager, customCommandRepository, properties.getCommandPrefix()));
        commandManager.register(new WatchtimeCommand());
        commandManager.register(new FollowageCommand());
        commandManager.register(new BroadcastCommand(broadcastRepository, eventLogService,
                properties.getBroadcastDefaultIntervalSeconds(), properties.getBroadcastDefaultMinMessages()));
        commandManager.register(new EventLogCommand(eventLogService));
        commandManager.register(new ModTimeoutCommand(eventLogService, moderationCaseRepository, moderationSyncTrigger));
        commandManager.register(new ModBanCommand(eventLogService, moderationCaseRepository, moderationSyncTrigger));
        commandManager.register(new ModUnbanCommand(eventLogService, moderationCaseRepository, moderationSyncTrigger));
        commandManager.register(new ModWarnCommand(moderationCaseRepository, moderationSyncTrigger));
        commandManager.register(new ModPurgeCommand(eventLogService));
        commandManager.register(new PermitCommand(eventLogService, properties.getAutomodPermitDefaultSeconds()));
        commandManager.register(new TitleCommand(eventLogService));
        commandManager.register(new GameCommand(eventLogService));
        commandManager.register(new ShoutoutCommand(eventLogService));
        commandManager.register(new ClipCommand());
        commandManager.register(new QuoteCommand(quoteRepository, eventLogService));
        commandManager.register(new PollCommand(pollManager, eventLogService));
        commandManager.register(new VoteCommand(pollManager));
        commandManager.register(new LinkCommand(pendingLinkVerificationRepository));
        commandManager.register(new VerifyCommand(accountLinkRepository, moderationBridgeClient, properties::getBridgeSettings));
        commandManager.register(new VanishCommand());
        LOGGER.info("{} built-in commands registered.", commandManager.getRegistry().size());
    }

    private void recordActivityQuietly(String channelLogin) {
        try {
            channelRepository.recordActivity(channelLogin);
            broadcastScheduler.recordActivity(channelLogin);
        } catch (Exception e) {
            LOGGER.warn("Could not save activity for channel {} (watchtime/broadcast tracking affected): {}",
                    channelLogin, e.getMessage());
        }
    }

    private void startDataRetentionTask() {
        List<PrunableResource> resources = List.of(
                PrunableResource.of("AutoMod-Eintraege", automodAdapter.getEngine()),
                PrunableResource.of("Event-Log-Eintraege", eventLogRepository));
        long intervalMinutes = properties.getDataRetentionIntervalMinutes();
        scheduleDataRetention(intervalMinutes, intervalMinutes,
                Duration.ofHours(properties.getDataRetentionHours()), resources);
    }

    private void startWatchtimeTracking(TwitchModerationApiClient moderationApiClient) {
        long intervalSeconds = properties.getWatchtimePollIntervalSeconds();
        scheduler.scheduleAtFixedRate(() -> {
            for (String channelLogin : channels) {
                try {
                    Optional<String> broadcasterId = moderationApiClient.resolveUserId(channelLogin);
                    if (broadcasterId.isEmpty()) {
                        continue;
                    }
                    for (TwitchModerationApiClient.ChatterRecord chatter
                            : moderationApiClient.getAllChatters(broadcasterId.get(), moderatorUserId)) {
                        if (chatter.userLogin().equalsIgnoreCase(config.twitchChatBotUsername())) {
                            continue;
                        }
                        watchtimeRepository.addSeconds(channelLogin, chatter.userId(), chatter.userLogin(), intervalSeconds);
                    }
                    Metrics.increment("twitch.watchtime_polls");
                } catch (Exception e) {
                    LOGGER.warn("Error recording watchtime for {}: {}", channelLogin, e.getMessage());
                }
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    private void startRoleReconciliation(TwitchModerationApiClient moderationApiClient) {
        long intervalMinutes = properties.getModerationSyncReconcileIntervalMinutes();
        scheduler.scheduleAtFixedRate(() -> {
            for (String channelLogin : channels) {
                try {
                    roleSyncService.reconcile(channelLogin, moderationApiClient);
                } catch (Exception e) {
                    LOGGER.warn("Error during role sync for {}: {}", channelLogin, e.getMessage());
                }
            }
        }, intervalMinutes, intervalMinutes, TimeUnit.MINUTES);
    }

    private void startHeartbeat() {
        scheduleHeartbeat(properties.getHeartbeatIntervalMinutes());
    }

    @Override
    protected String heartbeatSummary() {
        String metricsSnapshot = Metrics.snapshot().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("twitch."))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(" "));
        return "channels=" + channels.size() + " " + metricsSnapshot;
    }

    private void startBackupSchedule() {
        BackupSettings backupSettings = BackupSettings.from(properties::getRawProperty);
        backupService = new BackupService(database, backupSettings.resolveDirectory(properties.getDataDirectory()),
                "xbotenyy-twitch", backupSettings.maxBackupsToKeep());
        scheduleBackup(backupSettings, backupService);
    }

    @Override
    protected void onShutdown() {
        if (chatClient != null) {
            chatClient.close();
        }
        if (automodAdapter != null) {
            automodAdapter.shutdown();
        }
        if (discordWebhookClient != null) {
            discordWebhookClient.shutdown();
        }
        if (moderationBridgeServer != null) {
            moderationBridgeServer.stop();
        }
        if (backupService != null) {
            backupService.close();
        }
        if (database != null) {
            database.close();
        }
    }
}
