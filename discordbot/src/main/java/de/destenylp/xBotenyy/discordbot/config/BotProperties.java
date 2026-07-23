package de.destenylp.xBotenyy.discordbot.config;

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

public final class BotProperties {
    private static final Logger LOGGER = LoggerFactory.getLogger(BotProperties.class);

    private final Properties properties;

    private BotProperties(Properties properties) {
        this.properties = properties;
    }

    public static BotProperties load() {
        return load(resolvePath());
    }

    public static BotProperties load(Path file) {
        Map<String, String> defaults = defaultValues();

        if (!Files.exists(file)) {
            Properties created = new Properties();
            defaults.forEach(created::setProperty);
            writeToDisk(file, defaults);
            return new BotProperties(created);
        }

        Properties loaded = new Properties();
        try (InputStream in = Files.newInputStream(file);
             java.io.InputStreamReader reader = new java.io.InputStreamReader(in, StandardCharsets.UTF_8)) {
            loaded.load(reader);
        } catch (IOException e) {
            LOGGER.error("Konnte {} nicht laden, verwende Standardwerte: {}", file, e.getMessage());
            Properties fallback = new Properties();
            defaults.forEach(fallback::setProperty);
            return new BotProperties(fallback);
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

        return new BotProperties(loaded);
    }

    private static Path resolvePath() {
        String override = System.getenv("DISCORDBOT_PROPERTIES_FILE");
        if (override == null || override.isBlank()) {
            override = System.getProperty("discordbot.properties.file");
        }
        if (override == null || override.isBlank()) {
            override = "discordbot.properties";
        }
        return Path.of(override);
    }

    private static Map<String, String> defaultValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("data.directory", "data");
        values.put("database.file", "xbotenyy.sqlite");
        values.put("bot.activity.type", "PLAYING");
        values.put("bot.activity.text", "🛠️ Made by Desteny");
        values.put("scheduler.heartbeat.interval.minutes", "5");
        values.put("scheduler.ticket.autoclose.interval.minutes", "15");
        values.put("scheduler.giveaway.check.interval.minutes", "1");
        values.put("scheduler.data.retention.interval.minutes", "60");
        values.put("data.retention.hours", "24");
        values.put("jda.shutdown.timeout.seconds", "15");
        values.put("ticket.channel.delete.delay.seconds", "20");
        values.put("ticket.transcript.max.messages", "1000");
        values.put("ticket.default.max.open.per.member", "1");
        values.put("restaction.max.attempts", "3");
        values.put("restaction.base.delay.seconds", "2");
        values.put("giveaway.min.winners", "1");
        values.put("giveaway.max.winners", "20");
        values.put("giveaway.min.duration.seconds", "10");
        values.put("giveaway.max.duration.days", "60");
        values.put("eventlog.message-cache.max-size", "5000");
        values.put("eventlog.message-delete.content-max-length", "1000");
        values.put("bot.brand-color", "#5865F2");
        values.put("report.field.short-max-length", "100");
        values.put("report.field.long-max-length", "1000");
        values.put("ticket.field.subject-max-length", "100");
        values.put("ticket.field.description-max-length", "1000");
        values.put("ticket.field.close-reason-max-length", "500");
        values.put("ticket.autoclose.grace-period-divisor", "2");
        values.put("ticket.autoclose.grace-period-min-hours", "1");
        values.put("reactionrole.max-buttons-per-message", "25");
        values.put("welcome.preview-max-length", "150");
        values.put("socials.youtube.poll.interval.minutes", "10");
        values.put("socials.twitch.poll.interval.minutes", "1");
        values.put("socials.max.accounts.per.guild", "10");
        values.put("socials.http.timeout.seconds", "10");
        values.put("socials.twitch.token.refresh-buffer.seconds", "120");
        values.put("socials.youtube.default-message", "Neues Video von {account}!\\n{video.title}\\n{video.url}");
        values.put("socials.twitch.default-message", "{account} ist jetzt live auf Twitch!\\n{stream.title}\\n{stream.url}");

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

    private boolean getBoolean(String key, boolean fallback) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Boolean.parseBoolean(value.trim());
    }

    private double getDouble(String key, double fallback, double min, double max) {
        try {
            double value = Double.parseDouble(getString(key, String.valueOf(fallback)));
            return Math.min(max, Math.max(min, value));
        } catch (NumberFormatException e) {
            LOGGER.warn("Ungueltiger Wert fuer {}, verwende Standardwert {}", key, fallback);
            return fallback;
        }
    }

    private Set<String> getStringSet(String key) {
        String raw = properties.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isBlank()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    public Path getDataDirectory() {
        return Path.of(getString("data.directory", "data"));
    }

    public Path getDatabaseFile() {
        return getDataDirectory().resolve(getString("database.file", "xbotenyy.sqlite"));
    }

    public String getBotActivityType() {
        return getString("bot.activity.type", "PLAYING").toUpperCase();
    }

    public String getBotActivityText() {
        return getString("bot.activity.text", "🛠️ Made by Desteny");
    }

    public long getHeartbeatIntervalMinutes() {
        return getLong("scheduler.heartbeat.interval.minutes", 5, 1);
    }

    public long getTicketAutoCloseIntervalMinutes() {
        return getLong("scheduler.ticket.autoclose.interval.minutes", 15, 1);
    }

    public long getGiveawayCheckIntervalMinutes() {
        return getLong("scheduler.giveaway.check.interval.minutes", 1, 1);
    }

    public long getDataRetentionIntervalMinutes() {
        return getLong("scheduler.data.retention.interval.minutes", 60, 1);
    }

    public long getDataRetentionHours() {
        return getLong("data.retention.hours", 24, 1);
    }

    public long getJdaShutdownTimeoutSeconds() {
        return getLong("jda.shutdown.timeout.seconds", 15, 1);
    }

    public long getTicketChannelDeleteDelaySeconds() {
        return getLong("ticket.channel.delete.delay.seconds", 20, 0);
    }

    public int getTicketTranscriptMaxMessages() {
        return getInt("ticket.transcript.max.messages", 1000, 1);
    }

    public int getTicketDefaultMaxOpenTicketsPerMember() {
        return getInt("ticket.default.max.open.per.member", 1, 1);
    }

    public int getRestActionMaxAttempts() {
        return getInt("restaction.max.attempts", 3, 1);
    }

    public long getRestActionBaseDelaySeconds() {
        return getLong("restaction.base.delay.seconds", 2, 1);
    }

    public int getGiveawayMinWinners() {
        return getInt("giveaway.min.winners", 1, 1);
    }

    public int getGiveawayMaxWinners() {
        return getInt("giveaway.max.winners", 20, getGiveawayMinWinners());
    }

    public long getGiveawayMinDurationSeconds() {
        return getLong("giveaway.min.duration.seconds", 10, 1);
    }

    public long getGiveawayMaxDurationDays() {
        return getLong("giveaway.max.duration.days", 60, 1);
    }

    public int getEventLogMessageCacheMaxSize() {
        return getInt("eventlog.message-cache.max-size", 5000, 100);
    }

    public int getEventLogMessageDeleteContentMaxLength() {
        return Math.min(getInt("eventlog.message-delete.content-max-length", 1000, 100), 1024);
    }

    public String getBrandColorHex() {
        return getString("bot.brand-color", "#5865F2");
    }

    public int getReportFieldShortMaxLength() {
        return Math.min(getInt("report.field.short-max-length", 100, 1), 4000);
    }

    public int getReportFieldLongMaxLength() {
        return Math.min(getInt("report.field.long-max-length", 1000, 1), 4000);
    }

    public int getTicketFieldSubjectMaxLength() {
        return Math.min(getInt("ticket.field.subject-max-length", 100, 1), 4000);
    }

    public int getTicketFieldDescriptionMaxLength() {
        return Math.min(getInt("ticket.field.description-max-length", 1000, 1), 4000);
    }

    public int getTicketFieldCloseReasonMaxLength() {
        return Math.min(getInt("ticket.field.close-reason-max-length", 500, 1), 4000);
    }

    public int getTicketAutoCloseGracePeriodDivisor() {
        return getInt("ticket.autoclose.grace-period-divisor", 2, 1);
    }

    public int getTicketAutoCloseGracePeriodMinHours() {
        return getInt("ticket.autoclose.grace-period-min-hours", 1, 0);
    }

    public int getReactionRoleMaxButtonsPerMessage() {
        return Math.min(getInt("reactionrole.max-buttons-per-message", 25, 1), 25);
    }

    public int getWelcomePreviewMaxLength() {
        return getInt("welcome.preview-max-length", 150, 10);
    }

    public long getSocialsYoutubePollIntervalMinutes() {
        return getLong("socials.youtube.poll.interval.minutes", 10, 1);
    }

    public long getSocialsTwitchPollIntervalMinutes() {
        return getLong("socials.twitch.poll.interval.minutes", 1, 1);
    }

    public int getSocialsMaxAccountsPerGuild() {
        return getInt("socials.max.accounts.per.guild", 10, 1);
    }

    public long getSocialsHttpTimeoutSeconds() {
        return getLong("socials.http.timeout.seconds", 10, 1);
    }

    public long getSocialsTwitchTokenRefreshBufferSeconds() {
        return getLong("socials.twitch.token.refresh-buffer.seconds", 120, 0);
    }

    public String getSocialsYoutubeDefaultMessage() {
        return getString("socials.youtube.default-message", "");
    }

    public String getSocialsTwitchDefaultMessage() {
        return getString("socials.twitch.default-message", "");
    }

    public String getRawProperty(String key) {
        return properties.getProperty(key);
    }
}
