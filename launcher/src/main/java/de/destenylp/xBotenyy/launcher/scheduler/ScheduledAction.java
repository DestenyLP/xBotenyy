package de.destenylp.xBotenyy.launcher.scheduler;

import java.util.Locale;
import java.util.Optional;

public enum ScheduledAction {
    RESTART,
    STOP,
    START;

    public static Optional<ScheduledAction> parse(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(ScheduledAction.valueOf(token.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

