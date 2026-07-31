package de.destenylp.xBotenyy.launcher.scheduler;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {

    private static final Pattern PATTERN = Pattern.compile("^(\\d+)([smhd]?)$");

    private DurationParser() {
    }

    public static Duration parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Intervall darf nicht leer sein.");
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        Matcher matcher = PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Ungueltiges Intervall '" + raw
                    + "'. Format: <zahl>[s|m|h|d], z.B. 30m, 6h, 1d.");
        }
        long value = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2);
        Duration duration = switch (unit) {
            case "", "s" -> Duration.ofSeconds(value);
            case "m" -> Duration.ofMinutes(value);
            case "h" -> Duration.ofHours(value);
            case "d" -> Duration.ofDays(value);
            default -> throw new IllegalArgumentException("Unbekannte Zeiteinheit '" + unit + "'.");
        };
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("Intervall muss groesser als 0 sein.");
        }
        return duration;
    }

    public static String format(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds % 86400 == 0) {
            return (seconds / 86400) + "d";
        }
        if (seconds % 3600 == 0) {
            return (seconds / 3600) + "h";
        }
        if (seconds % 60 == 0) {
            return (seconds / 60) + "m";
        }
        return seconds + "s";
    }
}
