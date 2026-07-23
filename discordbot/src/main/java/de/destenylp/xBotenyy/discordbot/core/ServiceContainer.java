package de.destenylp.xBotenyy.discordbot.core;

import de.destenylp.xBotenyy.discordbot.automod.AutomodService;
import de.destenylp.xBotenyy.common.automod.AutomodSettings;
import de.destenylp.xBotenyy.common.automod.AutomodSettingsFactory;
import de.destenylp.xBotenyy.common.automod.ai.GroqSafeguardClient;
import de.destenylp.xBotenyy.common.config.CommonConfig;
import de.destenylp.xBotenyy.common.persistence.BackupService;
import de.destenylp.xBotenyy.common.persistence.BackupSettings;
import de.destenylp.xBotenyy.common.persistence.sql.Database;
import de.destenylp.xBotenyy.discordbot.config.BotProperties;
import de.destenylp.xBotenyy.discordbot.eventlog.EventLogManager;
import de.destenylp.xBotenyy.discordbot.eventlog.EventLogService;
import de.destenylp.xBotenyy.discordbot.giveaways.GiveawayManager;
import de.destenylp.xBotenyy.discordbot.giveaways.GiveawayService;
import de.destenylp.xBotenyy.discordbot.reactionroles.ReactionRoleManager;
import de.destenylp.xBotenyy.discordbot.reactionroles.ReactionRoleService;
import de.destenylp.xBotenyy.discordbot.reports.ReportManager;
import de.destenylp.xBotenyy.discordbot.reports.ReportService;
import de.destenylp.xBotenyy.discordbot.socials.SocialManager;
import de.destenylp.xBotenyy.discordbot.socials.SocialService;
import de.destenylp.xBotenyy.discordbot.tickets.TicketCloseCoordinator;
import de.destenylp.xBotenyy.discordbot.tickets.TicketManager;
import de.destenylp.xBotenyy.discordbot.tickets.TicketService;
import de.destenylp.xBotenyy.discordbot.welcome.WelcomeManager;
import de.destenylp.xBotenyy.discordbot.welcome.WelcomeService;

import java.time.Duration;
import java.util.List;

public class ServiceContainer implements AutoCloseable {
    private final Database database;
    private final ReactionRoleService reactionRoleService;
    private final WelcomeService welcomeService;
    private final ReportService reportService;
    private final TicketService ticketService;
    private final TicketCloseCoordinator ticketCloseCoordinator;
    private final GiveawayService giveawayService;
    private final EventLogService eventLogService;
    private final SocialService socialService;
    private final AutomodService automodService;
    private final BackupService backupService;
    private final BackupSettings backupSettings;

    public ServiceContainer() {
        this(BotProperties.load());
    }

    public ServiceContainer(BotProperties properties) {
        this(Database.open(properties.getDatabaseFile()), properties);
    }

    private ServiceContainer(Database database, BotProperties properties) {
        this(database,
                new ReactionRoleManager(database),
                new WelcomeManager(database),
                new ReportManager(database),
                new TicketManager(database, properties.getTicketDefaultMaxOpenTicketsPerMember()),
                new GiveawayManager(database),
                new EventLogManager(database),
                new SocialManager(database),
                properties);
    }

    ServiceContainer(Database database, ReactionRoleManager reactionRoleManager, WelcomeManager welcomeManager,
                      ReportManager reportManager, TicketManager ticketManager, GiveawayManager giveawayManager,
                      EventLogManager eventLogManager, SocialManager socialManager, BotProperties properties) {
        this.database = database;
        this.reactionRoleService = new ReactionRoleService(reactionRoleManager);
        this.welcomeService = new WelcomeService(welcomeManager);
        this.reportService = new ReportService(reportManager);
        this.ticketService = new TicketService(ticketManager, properties.getTicketAutoCloseGracePeriodDivisor(),
                properties.getTicketAutoCloseGracePeriodMinHours());
        this.ticketCloseCoordinator = new TicketCloseCoordinator(ticketService, properties.getTicketChannelDeleteDelaySeconds());
        this.giveawayService = new GiveawayService(giveawayManager, properties.getGiveawayMinWinners(),
                properties.getGiveawayMaxWinners(), Duration.ofSeconds(properties.getGiveawayMinDurationSeconds()),
                Duration.ofDays(properties.getGiveawayMaxDurationDays()));
        this.eventLogService = new EventLogService(eventLogManager);
        this.socialService = new SocialService(socialManager, properties.getSocialsMaxAccountsPerGuild());

        AutomodSettings automodSettings = AutomodSettingsFactory.from(properties::getRawProperty);
        this.automodService = new AutomodService(automodSettings, buildModerationClient(automodSettings));

        this.backupSettings = BackupSettings.from(properties::getRawProperty);
        this.backupService = new BackupService(database,
                backupSettings.resolveDirectory(properties.getDataDirectory()), "xbotenyy-discord",
                backupSettings.maxBackupsToKeep());
    }

    private static GroqSafeguardClient buildModerationClient(AutomodSettings settings) {
        if (!settings.getAiFilter().enabled()) {
            return null;
        }
        return CommonConfig.load()
                .filter(CommonConfig::hasGroqApiKey)
                .map(config -> new GroqSafeguardClient(config.groqApiKey(),
                        Duration.ofSeconds(Math.max(settings.getAiFilter().timeoutSeconds(), 1))))
                .orElse(null);
    }

    public List<GuildService> getAllServices() {
        return List.of(reactionRoleService, welcomeService, reportService, ticketService, giveawayService,
                eventLogService, socialService, automodService);
    }

    public List<PrunableGuildService> getPrunableServices() {
        return List.of(reportService, ticketService, giveawayService, automodService);
    }

    public AutomodService getAutomodService() {
        return automodService;
    }

    public BackupService getBackupService() {
        return backupService;
    }

    public BackupSettings getBackupSettings() {
        return backupSettings;
    }

    public ReactionRoleService getReactionRoleService() {
        return reactionRoleService;
    }

    public WelcomeService getWelcomeService() {
        return welcomeService;
    }

    public ReportService getReportService() {
        return reportService;
    }

    public TicketService getTicketService() {
        return ticketService;
    }

    public TicketCloseCoordinator getTicketCloseCoordinator() {
        return ticketCloseCoordinator;
    }

    public GiveawayService getGiveawayService() {
        return giveawayService;
    }

    public EventLogService getEventLogService() {
        return eventLogService;
    }

    public SocialService getSocialService() {
        return socialService;
    }

    @Override
    public void close() {
        automodService.shutdown();
        backupService.close();
        database.close();
    }
}
