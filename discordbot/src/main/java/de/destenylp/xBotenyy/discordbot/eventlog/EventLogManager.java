package de.destenylp.xBotenyy.discordbot.eventlog;

import de.destenylp.xBotenyy.common.persistence.sql.AbstractSqlManager;
import de.destenylp.xBotenyy.common.persistence.sql.Database;
import de.destenylp.xBotenyy.common.persistence.sql.Jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class EventLogManager extends AbstractSqlManager implements EventLogRepository {
    public EventLogManager(Database database) {
        super(database);
    }

    @Override
    public void setDefaultChannel(String guildId, String channelId) {
        database.runInTransaction(connection -> {
            ensureSettingsRow(connection, "eventlog_guild_settings", guildId);
            Jdbc.update(connection, "UPDATE eventlog_guild_settings SET default_channel_id = ? WHERE guild_id = ?",
                    channelId, guildId);
        });
    }

    @Override
    public void setEventEnabled(String guildId, LogEventType type, boolean enabled) {
        database.runInTransaction(connection -> upsertRule(connection, guildId, type, enabled, currentChannelId(connection, guildId, type)));
    }

    @Override
    public void setEventChannel(String guildId, LogEventType type, String channelId) {
        database.runInTransaction(connection -> upsertRule(connection, guildId, type, currentEnabled(connection, guildId, type), channelId));
    }

    @Override
    public void enableAll(String guildId, String defaultChannelId) {
        database.runInTransaction(connection -> {
            ensureSettingsRow(connection, "eventlog_guild_settings", guildId);
            Jdbc.update(connection, "UPDATE eventlog_guild_settings SET default_channel_id = ? WHERE guild_id = ?",
                    defaultChannelId, guildId);
            for (LogEventType type : LogEventType.values()) {
                upsertRule(connection, guildId, type, true, existingChannelId(connection, guildId, type));
            }
        });
    }

    private void upsertRule(Connection connection, String guildId, LogEventType type, boolean enabled, String channelId)
            throws SQLException {
        Jdbc.update(connection, """
                INSERT INTO eventlog_rules (guild_id, event_type, enabled, channel_id) VALUES (?, ?, ?, ?)
                ON CONFLICT(guild_id, event_type) DO UPDATE SET enabled = excluded.enabled, channel_id = excluded.channel_id
                """, guildId, type, enabled, channelId);
    }

    private boolean currentEnabled(Connection connection, String guildId, LogEventType type) throws SQLException {
        return Jdbc.queryOne(connection,
                "SELECT enabled FROM eventlog_rules WHERE guild_id = ? AND event_type = ?",
                rs -> Jdbc.getBoolean(rs, "enabled"), guildId, type).orElse(true);
    }

    private String currentChannelId(Connection connection, String guildId, LogEventType type) throws SQLException {
        return existingChannelId(connection, guildId, type);
    }

    private String existingChannelId(Connection connection, String guildId, LogEventType type) throws SQLException {
        return Jdbc.queryOne(connection, "SELECT channel_id FROM eventlog_rules WHERE guild_id = ? AND event_type = ?",
                rs -> rs.getString("channel_id"), guildId, type).orElse(null);
    }

    @Override
    public Optional<EventLogSettings> getSettingsFor(String guildId) {
        return database.withConnection(connection -> Jdbc.queryOne(connection,
                "SELECT * FROM eventlog_guild_settings WHERE guild_id = ?",
                resultSet -> mapSettings(connection, guildId, resultSet), guildId));
    }

    private EventLogSettings mapSettings(Connection connection, String guildId, ResultSet resultSet) throws SQLException {
        EventLogSettings settings = new EventLogSettings();
        settings.setDefaultChannelId(resultSet.getString("default_channel_id"));
        for (var row : Jdbc.query(connection, "SELECT * FROM eventlog_rules WHERE guild_id = ?",
                this::mapRuleRow, guildId)) {
            settings.putRule(row.type(), row.rule());
        }
        return settings;
    }

    private record RuleRow(LogEventType type, EventLogRule rule) {
    }

    private RuleRow mapRuleRow(ResultSet resultSet) throws SQLException {
        LogEventType type = LogEventType.valueOf(resultSet.getString("event_type"));
        EventLogRule rule = new EventLogRule(Jdbc.getBoolean(resultSet, "enabled"), resultSet.getString("channel_id"));
        return new RuleRow(type, rule);
    }
}
