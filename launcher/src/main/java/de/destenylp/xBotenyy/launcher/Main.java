package de.destenylp.xBotenyy.launcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public final class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final int DEFAULT_MAX_RESTART_ATTEMPTS = 5;
    private static final int DEFAULT_RESTART_DELAY_SECONDS = 15;

    private Main() {
    }

    public static void main(String[] args) throws InterruptedException {
        LaunchMode mode = LaunchMode.fromArgs(args);
        int maxRestartAttempts = resolveMaxRestartAttempts();
        Duration restartDelay = resolveRestartDelay();
        LOGGER.info("xBotenyy Launcher startet im Modus {} (maxRestartAttempts={} restartDelay={}s)",
                mode, maxRestartAttempts, restartDelay.toSeconds());

        Thread discordThread = mode.includesDiscord()
                ? startSupervised("discordbot-main", "Discord-Bot", maxRestartAttempts, restartDelay,
                        () -> de.destenylp.xBotenyy.discordbot.Main.main(new String[0]))
                : null;
        Thread twitchThread = mode.includesTwitch()
                ? startSupervised("twitchbot-main", "Twitch-Bot", maxRestartAttempts, restartDelay,
                        () -> de.destenylp.xBotenyy.twitchbot.Main.main(new String[0]))
                : null;

        if (discordThread == null && twitchThread == null) {
            LOGGER.error("Modus {} aktiviert keinen Bot, Launcher wird beendet.", mode);
            return;
        }

        joinQuietly(discordThread);
        joinQuietly(twitchThread);

        LOGGER.info("xBotenyy Launcher wurde beendet.");
    }

    private static Thread startSupervised(String threadName, String botName, int maxRestartAttempts,
                                           Duration restartDelay, Runnable entryPoint) {
        BotSupervisor supervisor = new BotSupervisor(botName, entryPoint, LOGGER, maxRestartAttempts, restartDelay);
        Thread thread = new Thread(supervisor::runSupervised, threadName);
        thread.setUncaughtExceptionHandler((failedThread, error) ->
                LOGGER.error("{} wurde durch einen fatalen Fehler im Thread {} beendet, der andere Bot ist davon "
                        + "nicht betroffen: ", botName, failedThread.getName(), error));
        thread.start();
        return thread;
    }

    private static void joinQuietly(Thread thread) throws InterruptedException {
        if (thread != null) {
            thread.join();
        }
    }

    private static int resolveMaxRestartAttempts() {
        return parsePositiveInt(System.getenv("LAUNCHER_MAX_RESTART_ATTEMPTS"), DEFAULT_MAX_RESTART_ATTEMPTS);
    }

    private static Duration resolveRestartDelay() {
        return Duration.ofSeconds(parsePositiveInt(System.getenv("LAUNCHER_RESTART_DELAY_SECONDS"),
                DEFAULT_RESTART_DELAY_SECONDS));
    }

    private static int parsePositiveInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
