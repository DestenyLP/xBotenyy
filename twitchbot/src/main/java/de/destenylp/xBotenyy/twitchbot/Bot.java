package de.destenylp.xBotenyy.twitchbot;

import de.destenylp.xBotenyy.common.automod.AutomodSettings;
import de.destenylp.xBotenyy.common.automod.AutomodSettingsFactory;
import de.destenylp.xBotenyy.common.automod.ai.GroqSafeguardClient;
import de.destenylp.xBotenyy.common.config.CommonConfig;
import de.destenylp.xBotenyy.common.core.AbstractBot;
import de.destenylp.xBotenyy.common.core.PrunableResource;
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
import de.destenylp.xBotenyy.twitchbot.commands.impl.AutomodStatusCommand;
import de.destenylp.xBotenyy.twitchbot.commands.impl.BroadcastCommand;
import de.destenylp.xBotenyy.twitchbot.commands.impl.CommandsCommand;
import de.destenylp.xBotenyy.twitchbot.commands.impl.CustomCommandManagementCommand;
import de.destenylp.xBotenyy.twitchbot.commands.impl.EventLogCommand;
import de.destenylp.xBotenyy.twitchbot.commands.impl.FollowageCommand;
import de.destenylp.xBotenyy.twitchbot.commands.impl.PingCommand;
import de.destenylp.xBotenyy.twitchbot.commands.impl.StrikesCommand;
import de.destenylp.xBotenyy.twitchbot.commands.impl.UptimeCommand;
import de.destenylp.xBotenyy.twitchbot.commands.impl.WatchtimeCommand;
import de.destenylp.xBotenyy.twitchbot.config.TwitchBotProperties;
import de.destenylp.xBotenyy.twitchbot.eventlog.TwitchEventLogService;
import de.destenylp.xBotenyy.twitchbot.persistence.CustomCommandRepository;
import de.destenylp.xBotenyy.twitchbot.persistence.TwitchBroadcastRepository;
import de.destenylp.xBotenyy.twitchbot.persistence.TwitchChannelRepository;
import de.destenylp.xBotenyy.twitchbot.persistence.TwitchEventLogRepository;
import de.destenylp.xBotenyy.twitchbot.persistence.TwitchWatchtimeRepository;
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
            LOGGER.error("Keine Twitch-Kanäle konfiguriert (twitch.chat.channels in twitchbot.properties). Beende.");
            return;
        }

        database = Database.open(properties.getDatabaseFile());
        channelRepository = new TwitchChannelRepository(database);
        watchtimeRepository = new TwitchWatchtimeRepository(database);
        eventLogRepository = new TwitchEventLogRepository(database);
        eventLogService = new TwitchEventLogService(eventLogRepository);
        broadcastRepository = new TwitchBroadcastRepository(database);
        CustomCommandRepository customCommandRepository = new CustomCommandRepository(database);
        channels.forEach(channelRepository::recordJoin);

        Optional<TwitchUserTokenManager> tokenManager = TwitchUserTokenManager.create(
                config.twitchClientId(), config.twitchClientSecret(), config.twitchBotRefreshToken(),
                config.envFilePath(), "TWITCH_BOT_REFRESH_TOKEN");

        java.util.function.Supplier<String> accessTokenSupplier = tokenManager
                .<java.util.function.Supplier<String>>map(manager -> manager::getAccessToken)
                .orElse(config::twitchModeratorAccessToken);

        if (tokenManager.isPresent()) {
            LOGGER.info("Twitch-Refresh-Token gefunden, Access-Token wird automatisch erneuert.");
            tokenManager.get().refreshNow();
        } else {
            LOGGER.warn("Kein TWITCH_BOT_REFRESH_TOKEN gesetzt - Access-Token laeuft alle paar Stunden ab "
                    + "und muss dann manuell erneuert werden. Siehe README fuer automatisches Setup.");
        }

        TwitchModerationApiClient moderationApiClient = new TwitchModerationApiClient(
                config.twitchClientId(), accessTokenSupplier, Duration.ofSeconds(10),
                properties.getRestActionMaxAttempts(), Duration.ofSeconds(properties.getRestActionBaseDelaySeconds()));

        moderatorUserId = moderationApiClient.resolveUserId(config.twitchChatBotUsername())
                .orElseThrow(() -> new IllegalStateException(
                        "Konnte die Twitch-Nutzer-ID des Bot-Accounts nicht aufloesen - "
                                + "TWITCH_BOT_USERNAME und TWITCH_MODERATOR_ACCESS_TOKEN pruefen."));

        TwitchAppAccessTokenManager appAccessTokenManager = new TwitchAppAccessTokenManager(
                config.twitchClientId(), config.twitchClientSecret(), Duration.ofSeconds(10), Duration.ofMinutes(10),
                properties.getRestActionMaxAttempts(), Duration.ofSeconds(properties.getRestActionBaseDelaySeconds()));

        chatClient = new TwitchChatClient(config.twitchClientId(), appAccessTokenManager, accessTokenSupplier,
                moderatorUserId, moderationApiClient::resolveUserId, channels, properties.getReconnectDelaySeconds(),
                properties.getMaxReconnectDelaySeconds(), Duration.ofSeconds(10),
                properties.getRestActionMaxAttempts(), Duration.ofSeconds(properties.getRestActionBaseDelaySeconds()));

        AutomodSettings automodSettings = AutomodSettingsFactory.from(properties::getRawProperty);
        GroqSafeguardClient moderationClient = automodSettings.getAiFilter().enabled() && config.hasGroqApiKey()
                ? new GroqSafeguardClient(config.groqApiKey(), Duration.ofSeconds(Math.max(automodSettings.getAiFilter().timeoutSeconds(), 1)))
                : null;

        automodAdapter = new TwitchAutomodAdapter(automodSettings, moderationClient, chatClient,
                moderationApiClient, properties.getWarnMessageTemplate(), moderatorUserId, eventLogService);

        TwitchBotServices services = new TwitchBotServices(chatClient, automodAdapter.getEngine(),
                customCommandRepository, watchtimeRepository, moderationApiClient, moderatorUserId, startedAt);
        commandManager = new TwitchCommandManager(properties.getCommandPrefix(), customCommandRepository, services,
                eventLogService);
        registerCommands(commandManager, customCommandRepository);

        broadcastScheduler = new TwitchBroadcastScheduler(broadcastRepository, chatClient, eventLogService, channels);
        broadcastScheduler.start(scheduler, properties.getBroadcastCheckIntervalSeconds());

        chatClient.onMessage(message -> {
            try {
                channelRepository.recordActivity(message.channelLogin());
                broadcastScheduler.recordActivity(message.channelLogin());
                Metrics.increment("twitch.messages_processed");
                boolean flaggedByAutomod = automodAdapter.handleMessage(message);
                if (!flaggedByAutomod) {
                    commandManager.handleMessage(message);
                }
            } catch (Exception e) {
                LOGGER.error("Unerwarteter Fehler bei der Verarbeitung einer Twitch-Nachricht in Kanal {}: ",
                        message.channelLogin(), e);
            }
        });
        chatClient.onConnected(() -> LOGGER.info("Twitch-Bot ist in {} Kanälen aktiv: {}", channels.size(), channels));

        startDataRetentionTask();
        startHeartbeat();
        startBackupSchedule();
        startWatchtimeTracking(moderationApiClient);

        registerShutdownHook();

        LOGGER.info("Starte Twitch-Bot als {} fuer Kanäle: {}", config.twitchChatBotUsername(), channels);
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
        LOGGER.info("{} eingebaute Befehle registriert.", commandManager.getRegistry().size());
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
                    LOGGER.warn("Fehler beim Erfassen der Watchtime fuer {}: {}", channelLogin, e.getMessage());
                }
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
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
        if (backupService != null) {
            backupService.close();
        }
        if (database != null) {
            database.close();
        }
    }
}