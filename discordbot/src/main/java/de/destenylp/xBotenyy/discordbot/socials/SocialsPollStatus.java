package de.destenylp.xBotenyy.discordbot.socials;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public final class SocialsPollStatus {
    private static final AtomicReference<Instant> LAST_YOUTUBE_POLL = new AtomicReference<>();
    private static final AtomicReference<Instant> LAST_TWITCH_POLL = new AtomicReference<>();
    private static final AtomicReference<String> LAST_YOUTUBE_ERROR = new AtomicReference<>();
    private static final AtomicReference<String> LAST_TWITCH_ERROR = new AtomicReference<>();

    private static volatile boolean twitchConfigured;
    private static volatile long youtubeIntervalMinutes;
    private static volatile long twitchIntervalMinutes;

    private SocialsPollStatus() {
    }

    public static void configure(boolean twitchCredentialsConfigured, long youtubePollIntervalMinutes,
                                  long twitchPollIntervalMinutes) {
        twitchConfigured = twitchCredentialsConfigured;
        youtubeIntervalMinutes = youtubePollIntervalMinutes;
        twitchIntervalMinutes = twitchPollIntervalMinutes;
    }

    public static void recordYoutubePollAttempt() {
        LAST_YOUTUBE_POLL.set(Instant.now());
    }

    public static void recordYoutubeError(String message) {
        LAST_YOUTUBE_ERROR.set(message);
    }

    public static void recordTwitchPollAttempt() {
        LAST_TWITCH_POLL.set(Instant.now());
    }

    public static void recordTwitchError(String message) {
        LAST_TWITCH_ERROR.set(message);
    }

    public static Instant getLastYoutubePoll() {
        return LAST_YOUTUBE_POLL.get();
    }

    public static Instant getLastTwitchPoll() {
        return LAST_TWITCH_POLL.get();
    }

    public static String getLastYoutubeError() {
        return LAST_YOUTUBE_ERROR.get();
    }

    public static String getLastTwitchError() {
        return LAST_TWITCH_ERROR.get();
    }

    public static boolean isTwitchConfigured() {
        return twitchConfigured;
    }

    public static long getYoutubeIntervalMinutes() {
        return youtubeIntervalMinutes;
    }

    public static long getTwitchIntervalMinutes() {
        return twitchIntervalMinutes;
    }
}
