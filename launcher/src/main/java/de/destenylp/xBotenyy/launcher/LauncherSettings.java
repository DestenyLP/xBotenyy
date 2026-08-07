package de.destenylp.xBotenyy.launcher;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class LauncherSettings {
    public static final int DEFAULT_MAX_RESTART_ATTEMPTS = 5;
    public static final int DEFAULT_RESTART_DELAY_SECONDS = 15;
    private static final String ENV_MAX_RESTART_ATTEMPTS = "LAUNCHER_MAX_RESTART_ATTEMPTS";
    private static final String ENV_RESTART_DELAY_SECONDS = "LAUNCHER_RESTART_DELAY_SECONDS";
    private final AtomicInteger maxRestartAttempts;
    private final AtomicLong restartDelaySeconds;

    private LauncherSettings(int maxRestartAttempts, long restartDelaySeconds) {
        this.maxRestartAttempts = new AtomicInteger(maxRestartAttempts);
        this.restartDelaySeconds = new AtomicLong(restartDelaySeconds);
    }

    public static LauncherSettings loadFromEnvironment() {
        int maxRestartAttempts = parsePositiveInt(System.getenv(ENV_MAX_RESTART_ATTEMPTS), DEFAULT_MAX_RESTART_ATTEMPTS);
        int restartDelaySeconds = parsePositiveInt(System.getenv(ENV_RESTART_DELAY_SECONDS), DEFAULT_RESTART_DELAY_SECONDS);
        return new LauncherSettings(maxRestartAttempts, restartDelaySeconds);
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

    public int getMaxRestartAttempts() {
        return maxRestartAttempts.get();
    }

    public int setMaxRestartAttempts(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("maxRestartAttempts muss >= 1 sein, war: " + value);
        }
        return maxRestartAttempts.getAndSet(value);
    }

    public long getRestartDelaySeconds() {
        return restartDelaySeconds.get();
    }

    public long setRestartDelaySeconds(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("restartDelaySeconds darf nicht negativ sein, war: " + value);
        }
        return restartDelaySeconds.getAndSet(value);
    }

    @Override
    public String toString() {
        return "maxRestartAttempts=" + getMaxRestartAttempts() + ", restartDelaySeconds=" + getRestartDelaySeconds();
    }
}

