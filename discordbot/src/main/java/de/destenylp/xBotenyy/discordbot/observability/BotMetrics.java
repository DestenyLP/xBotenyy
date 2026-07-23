package de.destenylp.xBotenyy.discordbot.observability;

import de.destenylp.xBotenyy.common.observability.Metrics;

public final class BotMetrics {
    private static final String COMMANDS_EXECUTED = "discord.commands_executed";
    private static final String REPORTS_CREATED = "discord.reports_created";
    private static final String REACTION_ROLES_ASSIGNED = "discord.reaction_roles_assigned";
    private static final String WELCOME_MESSAGES_SENT = "discord.welcome_messages_sent";
    private static final String TICKETS_CREATED = "discord.tickets_created";
    private static final String TICKETS_CLOSED = "discord.tickets_closed";
    private static final String TICKETS_AUTO_CLOSED = "discord.tickets_auto_closed";
    private static final String GIVEAWAYS_CREATED = "discord.giveaways_created";
    private static final String GIVEAWAYS_ENDED = "discord.giveaways_ended";
    private static final String EVENT_LOGS_SENT = "discord.event_logs_sent";
    private static final String YOUTUBE_VIDEOS_ANNOUNCED = "discord.youtube_videos_announced";
    private static final String TWITCH_STREAMS_ANNOUNCED = "discord.twitch_streams_announced";
    private static final String AUTOMOD_VIOLATIONS_DETECTED = "discord.automod_violations_detected";

    private BotMetrics() {
    }

    public static void incrementCommandsExecuted() {
        Metrics.increment(COMMANDS_EXECUTED);
    }

    public static void incrementReportsCreated() {
        Metrics.increment(REPORTS_CREATED);
    }

    public static void incrementReactionRolesAssigned() {
        Metrics.increment(REACTION_ROLES_ASSIGNED);
    }

    public static void incrementWelcomeMessagesSent() {
        Metrics.increment(WELCOME_MESSAGES_SENT);
    }

    public static void incrementTicketsCreated() {
        Metrics.increment(TICKETS_CREATED);
    }

    public static void incrementTicketsClosed() {
        Metrics.increment(TICKETS_CLOSED);
    }

    public static void incrementTicketsAutoClosed() {
        Metrics.increment(TICKETS_AUTO_CLOSED);
    }

    public static void incrementGiveawaysCreated() {
        Metrics.increment(GIVEAWAYS_CREATED);
    }

    public static void incrementGiveawaysEnded() {
        Metrics.increment(GIVEAWAYS_ENDED);
    }

    public static void incrementEventLogsSent() {
        Metrics.increment(EVENT_LOGS_SENT);
    }

    public static void incrementYoutubeVideosAnnounced() {
        Metrics.increment(YOUTUBE_VIDEOS_ANNOUNCED);
    }

    public static void incrementTwitchStreamsAnnounced() {
        Metrics.increment(TWITCH_STREAMS_ANNOUNCED);
    }

    public static void incrementAutomodViolationsDetected() {
        Metrics.increment(AUTOMOD_VIOLATIONS_DETECTED);
    }

    public static long getCommandsExecuted() {
        return Metrics.get(COMMANDS_EXECUTED);
    }

    public static long getReportsCreated() {
        return Metrics.get(REPORTS_CREATED);
    }

    public static long getReactionRolesAssigned() {
        return Metrics.get(REACTION_ROLES_ASSIGNED);
    }

    public static long getWelcomeMessagesSent() {
        return Metrics.get(WELCOME_MESSAGES_SENT);
    }

    public static long getTicketsCreated() {
        return Metrics.get(TICKETS_CREATED);
    }

    public static long getTicketsClosed() {
        return Metrics.get(TICKETS_CLOSED);
    }

    public static long getTicketsAutoClosed() {
        return Metrics.get(TICKETS_AUTO_CLOSED);
    }

    public static long getGiveawaysCreated() {
        return Metrics.get(GIVEAWAYS_CREATED);
    }

    public static long getGiveawaysEnded() {
        return Metrics.get(GIVEAWAYS_ENDED);
    }

    public static long getEventLogsSent() {
        return Metrics.get(EVENT_LOGS_SENT);
    }

    public static long getYoutubeVideosAnnounced() {
        return Metrics.get(YOUTUBE_VIDEOS_ANNOUNCED);
    }

    public static long getTwitchStreamsAnnounced() {
        return Metrics.get(TWITCH_STREAMS_ANNOUNCED);
    }

    public static long getAutomodViolationsDetected() {
        return Metrics.get(AUTOMOD_VIOLATIONS_DETECTED);
    }
}
