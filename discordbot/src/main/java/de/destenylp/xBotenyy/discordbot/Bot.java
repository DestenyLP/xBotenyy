package de.destenylp.xBotenyy.discordbot;

import de.destenylp.xBotenyy.common.config.CommonConfig;
import de.destenylp.xBotenyy.common.core.AbstractBot;
import de.destenylp.xBotenyy.common.core.PrunableResource;
import de.destenylp.xBotenyy.common.moderation.bridge.BridgeSettings;
import de.destenylp.xBotenyy.common.moderation.bridge.ModerationBridgeServer;
import de.destenylp.xBotenyy.discordbot.commands.*;
import de.destenylp.xBotenyy.discordbot.commands.report.MyReportsCommand;
import de.destenylp.xBotenyy.discordbot.commands.report.ReportCommand;
import de.destenylp.xBotenyy.discordbot.commands.user.InfoCommand;
import de.destenylp.xBotenyy.discordbot.commands.user.PingCommand;
import de.destenylp.xBotenyy.discordbot.config.BotProperties;
import de.destenylp.xBotenyy.discordbot.core.ServiceContainer;
import de.destenylp.xBotenyy.discordbot.giveaways.GiveawayEndCoordinator;
import de.destenylp.xBotenyy.discordbot.giveaways.GiveawayEndTask;
import de.destenylp.xBotenyy.discordbot.listeners.*;
import de.destenylp.xBotenyy.discordbot.moderation.DiscordModerationBridgeHandler;
import de.destenylp.xBotenyy.discordbot.observability.BotMetrics;
import de.destenylp.xBotenyy.discordbot.placeholders.DefaultPlaceholders;
import de.destenylp.xBotenyy.discordbot.reactionroles.ReactionRoleService;
import de.destenylp.xBotenyy.discordbot.socials.SocialService;
import de.destenylp.xBotenyy.discordbot.socials.SocialsPollStatus;
import de.destenylp.xBotenyy.discordbot.socials.twitch.TwitchApiClient;
import de.destenylp.xBotenyy.discordbot.socials.twitch.TwitchCheckTask;
import de.destenylp.xBotenyy.discordbot.socials.youtube.YoutubeCheckTask;
import de.destenylp.xBotenyy.discordbot.socials.youtube.YoutubeFeedClient;
import de.destenylp.xBotenyy.discordbot.tickets.TicketAutoCloseTask;
import de.destenylp.xBotenyy.discordbot.tickets.TicketTranscriptService;
import de.destenylp.xBotenyy.discordbot.util.DiscordColors;
import de.destenylp.xBotenyy.discordbot.util.RetryingRestAction;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Bot extends AbstractBot {
    private static final Logger LOGGER = LoggerFactory.getLogger(Bot.class);
    private static final String FALLBACK_VERSION = "dev";

    private final BotProperties properties;
    private DiscordCommandManager commandManager;
    private ServiceContainer services;
    private JDA jda;
    private ModerationBridgeServer moderationBridgeServer;
    private YoutubeFeedClient youtubeFeedClient;
    private TwitchApiClient twitchApiClient;

    public Bot(CommonConfig config) {
        this(config, BotProperties.load());
    }

    public Bot(CommonConfig config, BotProperties properties) {
        super(LOGGER, config, "bot-heartbeat", 1);
        this.properties = properties;
    }

    private Activity resolveActivity() {
        String text = properties.getBotActivityText();
        return switch (properties.getBotActivityType()) {
            case "WATCHING" -> Activity.watching(text);
            case "LISTENING" -> Activity.listening(text);
            case "COMPETING" -> Activity.competing(text);
            default -> Activity.playing(text);
        };
    }

    public void start() throws InterruptedException {
        DefaultPlaceholders.registerAll();

        RetryingRestAction.configure(properties.getRestActionMaxAttempts(), properties.getRestActionBaseDelaySeconds());
        TicketTranscriptService.configure(properties.getTicketTranscriptMaxMessages(),
                properties.getDataDirectory().resolve("transcripts"));
        DiscordColors.configure(DiscordColors.parseOrDefault(properties.getBrandColorHex(), DiscordColors.BLURPLE));
        ReactionRoleService.configureMaxButtonsPerMessage(properties.getReactionRoleMaxButtonsPerMessage());

        services = new ServiceContainer(properties);
        commandManager = new DiscordCommandManager(services.getEventLogService());

        LOGGER.info("Configuration loaded: Twitch integration={} Groq moderation key={} AutoMod AI feature={}",
                config.hasTwitchAppCredentials() ? "detected" : "not set",
                config.hasGroqApiKey() ? "detected" : "not set",
                services.getAutomodService().getSettings().getAiFilter().enabled() ? "enabled" : "disabled");

        Duration socialsHttpTimeout = Duration.ofSeconds(properties.getSocialsHttpTimeoutSeconds());
        int retryMaxAttempts = properties.getRestActionMaxAttempts();
        Duration retryBaseDelay = Duration.ofSeconds(properties.getRestActionBaseDelaySeconds());
        youtubeFeedClient = new YoutubeFeedClient(socialsHttpTimeout, retryMaxAttempts, retryBaseDelay);
        twitchApiClient = config.hasTwitchAppCredentials()
                ? new TwitchApiClient(config.twitchClientId(), config.twitchClientSecret(), socialsHttpTimeout,
                Duration.ofSeconds(properties.getSocialsTwitchTokenRefreshBufferSeconds()),
                retryMaxAttempts, retryBaseDelay)
                : null;

        registerCommands(services);

        jda = JDABuilder.createDefault(config.discordBotToken())
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .setChunkingFilter(ChunkingFilter.ALL)
                .disableCache(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS,
                        CacheFlag.EMOJI, CacheFlag.STICKER, CacheFlag.SCHEDULED_EVENTS,
                        CacheFlag.ROLE_TAGS, CacheFlag.FORUM_TAGS)
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
                .setActivity(resolveActivity())
                .addEventListeners(
                        new CommandListener(commandManager),
                        new ReactionRoleListener(services.getReactionRoleService()),
                        new WelcomeListener(services.getWelcomeService()),
                        new ReportListener(services.getReportService(),
                                properties.getReportFieldShortMaxLength(), properties.getReportFieldLongMaxLength()),
                        new TicketListener(services.getTicketService(), services.getTicketCloseCoordinator(),
                                properties.getTicketFieldSubjectMaxLength(), properties.getTicketFieldDescriptionMaxLength(),
                                properties.getTicketFieldCloseReasonMaxLength()),
                        new GiveawayListener(services.getGiveawayService()),
                        new EventLogListener(services.getEventLogService(),
                                properties.getEventLogMessageCacheMaxSize(),
                                properties.getEventLogMessageDeleteContentMaxLength()),
                        new AutomodListener(services.getAutomodService()),
                        new AccountLinkListener(services.getAccountLinkService()))
                .build();

        registerShutdownHook();
        jda.awaitReady();
        LOGGER.info("Discord Bot has been enabled.");

        BridgeSettings bridgeSettings = properties.getBridgeSettings();
        if (bridgeSettings.isServerEnabled()) {
            DiscordModerationBridgeHandler bridgeHandler = new DiscordModerationBridgeHandler(jda,
                    properties.getModerationSyncGuildId(), services.getModerationService(),
                    services.getAccountLinkRepository(), services.getPendingLinkVerificationRepository());
            moderationBridgeServer = new ModerationBridgeServer(bridgeSettings, bridgeHandler);
            moderationBridgeServer.start();
        }

        registerSlashCommands();
        startHeartbeat();
        startTicketAutoCloseTask();
        startGiveawayEndTask();
        startSocialsTasks();
        startDataRetentionTask();
        startBackupSchedule();
    }

    private void startBackupSchedule() {
        scheduleBackup(services.getBackupSettings(), services.getBackupService());
    }

    private void startHeartbeat() {
        scheduleHeartbeat(properties.getHeartbeatIntervalMinutes());
    }

    @Override
    protected String heartbeatSummary() {
        return String.format(
                "status=%s gatewayPingMs=%s commandsExecuted=%s reportsCreated=%s reactionRolesAssigned=%s "
                        + "welcomeMessagesSent=%s ticketsCreated=%s ticketsClosed=%s ticketsAutoClosed=%s "
                        + "giveawaysCreated=%s giveawaysEnded=%s eventLogsSent=%s youtubeVideosAnnounced=%s "
                        + "twitchStreamsAnnounced=%s automodViolationsDetected=%s",
                jda.getStatus(), jda.getGatewayPing(),
                BotMetrics.getCommandsExecuted(), BotMetrics.getReportsCreated(),
                BotMetrics.getReactionRolesAssigned(), BotMetrics.getWelcomeMessagesSent(),
                BotMetrics.getTicketsCreated(), BotMetrics.getTicketsClosed(), BotMetrics.getTicketsAutoClosed(),
                BotMetrics.getGiveawaysCreated(), BotMetrics.getGiveawaysEnded(), BotMetrics.getEventLogsSent(),
                BotMetrics.getYoutubeVideosAnnounced(), BotMetrics.getTwitchStreamsAnnounced(),
                BotMetrics.getAutomodViolationsDetected());
    }

    private void startTicketAutoCloseTask() {
        TicketAutoCloseTask task = new TicketAutoCloseTask(jda, services.getTicketService(), services.getTicketCloseCoordinator());
        scheduler.scheduleAtFixedRate(task, properties.getTicketAutoCloseIntervalMinutes(),
                properties.getTicketAutoCloseIntervalMinutes(), TimeUnit.MINUTES);
    }

    private void startGiveawayEndTask() {
        GiveawayEndTask task = new GiveawayEndTask(jda, services.getGiveawayService(), new GiveawayEndCoordinator());
        scheduler.scheduleAtFixedRate(task, properties.getGiveawayCheckIntervalMinutes(),
                properties.getGiveawayCheckIntervalMinutes(), TimeUnit.MINUTES);
    }

    private void startSocialsTasks() {
        SocialService socialService = services.getSocialService();

        SocialsPollStatus.configure(config.hasTwitchAppCredentials(), properties.getSocialsYoutubePollIntervalMinutes(),
                properties.getSocialsTwitchPollIntervalMinutes());

        YoutubeCheckTask youtubeTask = new YoutubeCheckTask(jda, socialService, youtubeFeedClient);
        scheduler.scheduleAtFixedRate(youtubeTask, properties.getSocialsYoutubePollIntervalMinutes(),
                properties.getSocialsYoutubePollIntervalMinutes(), TimeUnit.MINUTES);

        TwitchCheckTask twitchTask = new TwitchCheckTask(jda, socialService, twitchApiClient);
        scheduler.scheduleAtFixedRate(twitchTask, properties.getSocialsTwitchPollIntervalMinutes(),
                properties.getSocialsTwitchPollIntervalMinutes(), TimeUnit.MINUTES);
    }

    private void startDataRetentionTask() {
        List<PrunableResource> resources = services.getPrunableServices().stream()
                .map(service -> PrunableResource.of(service.getServiceName(), service::pruneOldEntries))
                .toList();
        scheduleDataRetention(0, properties.getDataRetentionIntervalMinutes(),
                Duration.ofHours(properties.getDataRetentionHours()), resources);
    }

    @Override
    protected void onShutdown() {
        if (moderationBridgeServer != null) {
            moderationBridgeServer.stop();
        }
        if (jda == null) {
            return;
        }
        jda.shutdown();
        try {
            long timeoutSeconds = properties.getJdaShutdownTimeoutSeconds();
            if (!jda.awaitShutdown(Duration.ofSeconds(timeoutSeconds))) {
                LOGGER.warn("JDA could not shut down cleanly within {}s, forcing shutdown.", timeoutSeconds);
                jda.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            jda.shutdownNow();
        }
        if (services != null) {
            services.close();
        }
    }

    private void registerCommands(ServiceContainer services) {
        commandManager.register(new PingCommand());
        commandManager.register(new InfoCommand(this));
        commandManager.register(new MessageCommand());
        commandManager.register(new ReactionRoleCommand(services.getReactionRoleService()));
        commandManager.register(new WelcomeCommand(services.getWelcomeService(), properties.getWelcomePreviewMaxLength()));
        commandManager.register(new ReportCommand(services.getReportService()));
        commandManager.register(new MyReportsCommand(services.getReportService()));
        commandManager.register(new TicketCommand(services.getTicketService(), services.getTicketCloseCoordinator()));
        commandManager.register(new GiveawayCommand(services.getGiveawayService()));
        commandManager.register(new EventLogCommand(services.getEventLogService()));
        commandManager.register(new SocialsCommand(services.getSocialService(),
                properties.getSocialsYoutubeDefaultMessage(), properties.getSocialsTwitchDefaultMessage(),
                youtubeFeedClient, twitchApiClient));
        commandManager.register(new AutomodCommand(services.getAutomodService()));
        commandManager.register(new ModerationCommand(services.getModerationService(),
                services.getModerationRoleSettingsRepository(), services.getModerationCaseRepository(),
                services.getModerationSyncTrigger()));
        commandManager.register(new ModerationRolesCommand(services.getModerationRoleSettingsRepository()));
        commandManager.register(new AccountLinkCommand(services.getAccountLinkService(),
                services.getModerationBridgeClient(), services.getProperties()::getBridgeSettings,
                services.getModerationRoleSettingsRepository()));
    }

    private void registerSlashCommands() {
        LOGGER.info("Registering {} Slash Commands", commandManager.size());
        jda.updateCommands()
                .addCommands(commandManager.allCommandData())
                .queue(success -> LOGGER.info("Slash commands registered successfully!"),
                        failure -> LOGGER.error("Failed to register slash commands: ", failure));
    }

    public String getVersion() {
        String implementationVersion = getClass().getPackage().getImplementationVersion();
        return implementationVersion != null ? implementationVersion : FALLBACK_VERSION;
    }
}
