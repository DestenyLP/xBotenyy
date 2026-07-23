package de.destenylp.xBotenyy.twitchbot.config;

import de.destenylp.xBotenyy.common.automod.AutomodSettingsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public final class TwitchBotProperties {
    private static final Logger LOGGER = LoggerFactory.getLogger(TwitchBotProperties.class);

    private final Properties properties;

    private TwitchBotProperties(Properties properties) {
        this.properties = properties;
    }

    public static TwitchBotProperties load() {
        return load(resolvePath());
    }

    public static TwitchBotProperties load(Path file) {
        Map<String, String> defaults = defaultValues();

        if (!Files.exists(file)) {
            Properties created = new Properties();
            defaults.forEach(created::setProperty);
            writeToDisk(file, defaults);
            return new TwitchBotProperties(created);
        }

        Properties loaded = new Properties();
        try (InputStream in = Files.newInputStream(file);
             java.io.InputStreamReader reader = new java.io.InputStreamReader(in, StandardCharsets.UTF_8)) {
            loaded.load(reader);
        } catch (IOException e) {
            LOGGER.error("Konnte {} nicht laden, verwende Standardwerte: {}", file, e.getMessage());
            Properties fallback = new Properties();
            defaults.forEach(fallback::setProperty);
            return new TwitchBotProperties(fallback);
        }

        boolean missingKey = false;
        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            if (!loaded.containsKey(entry.getKey())) {
                loaded.setProperty(entry.getKey(), entry.getValue());
                missingKey = true;
            }
        }
        if (missingKey) {
            Map<String, String> merged = new LinkedHashMap<>();
            for (String key : defaults.keySet()) {
                merged.put(key, loaded.getProperty(key));
            }
            writeToDisk(file, merged);
        }

        return new TwitchBotProperties(loaded);
    }

    private static Path resolvePath() {
        String override = System.getenv("TWITCHBOT_PROPERTIES_FILE");
        if (override == null || override.isBlank()) {
            override = System.getProperty("twitchbot.properties.file");
        }
        if (override == null || override.isBlank()) {
            override = "twitchbot.properties";
        }
        return Path.of(override);
    }

    private static Map<String, String> defaultValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("twitch.chat.channels", "");
        values.put("twitch.chat.reconnect.delay.seconds", "10");
        values.put("twitch.chat.max-reconnect-delay.seconds", "120");
        values.put("twitch.chat.warn.message", "⚠️ @{user} deine Nachricht wurde von AutoMod entfernt: {reason}");
        values.put("twitch.chat.command.prefix", "!");
        values.put("data.directory", "data");
        values.put("twitch.database.file", "xbotenyy-twitch.sqlite");
        values.put("data.retention.hours", "24");
        values.put("scheduler.data.retention.interval.minutes", "60");
        values.put("scheduler.heartbeat.interval.minutes", "5");
        values.put("restaction.max.attempts", "3");
        values.put("restaction.base.delay.seconds", "2");
        values.put("twitch.watchtime.poll.interval.seconds", "60");
        values.put("twitch.broadcast.check.interval.seconds", "30");
        values.put("twitch.broadcast.default.interval.seconds", "1800");
        values.put("twitch.broadcast.default.min.messages", "5");

        values.putAll(AutomodSettingsFactory.defaultValues());

        values.putAll(de.destenylp.xBotenyy.common.persistence.BackupSettings.defaultValues());
        return values;
    }

    private static void writeToDisk(Path file, Map<String, String> values) {
        try {
            if (file.toAbsolutePath().getParent() != null) {
                Files.createDirectories(file.toAbsolutePath().getParent());
            }
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, String> entry : values.entrySet()) {
                String value = entry.getValue() != null ? entry.getValue() : defaultValues().get(entry.getKey());
                builder.append(entry.getKey()).append('=').append(value).append(System.lineSeparator());
            }
            Files.writeString(file, builder.toString(), StandardCharsets.UTF_8);
            LOGGER.info("Konfigurationsdatei {} wurde geschrieben", file);
        } catch (IOException e) {
            LOGGER.error("Konnte {} nicht erstellen: {}", file, e.getMessage());
        }
    }

    private String getString(String key, String fallback) {
        String value = properties.getProperty(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private long getLong(String key, long fallback, long min) {
        try {
            return Math.max(min, Long.parseLong(getString(key, String.valueOf(fallback))));
        } catch (NumberFormatException e) {
            LOGGER.warn("Ungueltiger Wert fuer {}, verwende Standardwert {}", key, fallback);
            return fallback;
        }
    }

    private int getInt(String key, int fallback, int min) {
        try {
            return Math.max(min, Integer.parseInt(getString(key, String.valueOf(fallback))));
        } catch (NumberFormatException e) {
            LOGGER.warn("Ungueltiger Wert fuer {}, verwende Standardwert {}", key, fallback);
            return fallback;
        }
    }

    public String getRawProperty(String key) {
        return properties.getProperty(key);
    }

    public Set<String> getChatChannels() {
        String raw = getString("twitch.chat.channels", "");
        Set<String> channels = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim().toLowerCase(java.util.Locale.ROOT).replace("#", "");
            if (!trimmed.isBlank()) {
                channels.add(trimmed);
            }
        }
        return channels;
    }

    public long getReconnectDelaySeconds() {
        return getLong("twitch.chat.reconnect.delay.seconds", 10, 1);
    }

    public long getMaxReconnectDelaySeconds() {
        return getLong("twitch.chat.max-reconnect-delay.seconds", 120, getReconnectDelaySeconds());
    }

    public String getWarnMessageTemplate() {
        return getString("twitch.chat.warn.message", "⚠️ @{user} deine Nachricht wurde von AutoMod entfernt: {reason}");
    }

    public long getDataRetentionHours() {
        return getLong("data.retention.hours", 24, 1);
    }

    public long getDataRetentionIntervalMinutes() {
        return getLong("scheduler.data.retention.interval.minutes", 60, 1);
    }

    public long getHeartbeatIntervalMinutes() {
        return getLong("scheduler.heartbeat.interval.minutes", 5, 1);
    }

    public int getRestActionMaxAttempts() {
        return getInt("restaction.max.attempts", 3, 1);
    }

    public long getRestActionBaseDelaySeconds() {
        return getLong("restaction.base.delay.seconds", 2, 1);
    }

    public String getCommandPrefix() {
        return getString("twitch.chat.command.prefix", "!");
    }

    public long getWatchtimePollIntervalSeconds() {
        return getLong("twitch.watchtime.poll.interval.seconds", 60, 15);
    }

    public long getBroadcastCheckIntervalSeconds() {
        return getLong("twitch.broadcast.check.interval.seconds", 30, 5);
    }

    public long getBroadcastDefaultIntervalSeconds() {
        return getLong("twitch.broadcast.default.interval.seconds", 1800, 30);
    }

    public int getBroadcastDefaultMinMessages() {
        return getInt("twitch.broadcast.default.min.messages", 5, 0);
    }

    public Path getDataDirectory() {
        return Path.of(getString("data.directory", "data"));
    }

    public Path getDatabaseFile() {
        return getDataDirectory().resolve(getString("twitch.database.file", "xbotenyy-twitch.sqlite"));
    }
}
