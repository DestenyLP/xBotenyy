package de.destenylp.xBotenyy.common.persistence;

import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public record BackupSettings(boolean enabled, Duration interval, int maxBackupsToKeep, String directory) {
    public static BackupSettings from(Function<String, String> resolver) {
        boolean enabled = bool(resolver, "backup.enabled", true);
        long hours = longVal(resolver, "backup.interval.hours", 12);
        int keep = intVal(resolver, "backup.retention.count", 14);
        String directory = string(resolver, "backup.directory", "backups");
        return new BackupSettings(enabled, Duration.ofHours(Math.max(hours, 1)), Math.max(keep, 1), directory);
    }

    public static Map<String, String> defaultValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("backup.enabled", "true");
        values.put("backup.interval.hours", "12");
        values.put("backup.retention.count", "14");
        values.put("backup.directory", "backups");
        return values;
    }

    public Path resolveDirectory(Path dataDirectory) {
        Path path = Path.of(directory);
        return path.isAbsolute() ? path : dataDirectory.resolve(path);
    }

    private static String string(Function<String, String> resolver, String key, String fallback) {
        String value = resolver.apply(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static boolean bool(Function<String, String> resolver, String key, boolean fallback) {
        String value = resolver.apply(key);
        return value == null || value.isBlank() ? fallback : Boolean.parseBoolean(value.trim());
    }

    private static int intVal(Function<String, String> resolver, String key, int fallback) {
        try {
            String value = resolver.apply(key);
            return value == null || value.isBlank() ? fallback : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static long longVal(Function<String, String> resolver, String key, long fallback) {
        try {
            String value = resolver.apply(key);
            return value == null || value.isBlank() ? fallback : Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
