package de.destenylp.xBotenyy.twitchbot.persistence;

import de.destenylp.xBotenyy.common.persistence.sql.AbstractSqlManager;
import de.destenylp.xBotenyy.common.persistence.sql.Database;
import de.destenylp.xBotenyy.common.persistence.sql.Jdbc;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class CustomCommandRepository extends AbstractSqlManager {
    public CustomCommandRepository(Database database) {
        super(database);
    }

    public void upsert(String channelLogin, String name, String response, String createdBy) {
        long now = Instant.now().toEpochMilli();
        String normalizedName = normalize(name);
        database.useConnection(connection -> Jdbc.update(connection,
                "INSERT INTO twitch_custom_commands (channel_login, name, response, created_by, created_at, updated_at, uses) "
                        + "VALUES (?, ?, ?, ?, ?, ?, 0) "
                        + "ON CONFLICT(channel_login, name) DO UPDATE SET response = excluded.response, updated_at = excluded.updated_at",
                channelLogin, normalizedName, response, createdBy, now, now));
    }

    public boolean remove(String channelLogin, String name) {
        return database.withConnection(connection -> Jdbc.update(connection,
                "DELETE FROM twitch_custom_commands WHERE channel_login = ? AND name = ?",
                channelLogin, normalize(name))) > 0;
    }

    public Optional<CustomCommandRecord> find(String channelLogin, String name) {
        return database.withConnection(connection -> Jdbc.queryOne(connection,
                "SELECT name, response, created_by, created_at, uses FROM twitch_custom_commands "
                        + "WHERE channel_login = ? AND name = ?",
                CustomCommandRepository::mapRow, channelLogin, normalize(name)));
    }

    public List<CustomCommandRecord> list(String channelLogin) {
        return database.withConnection(connection -> Jdbc.query(connection,
                "SELECT name, response, created_by, created_at, uses FROM twitch_custom_commands "
                        + "WHERE channel_login = ? ORDER BY name ASC",
                CustomCommandRepository::mapRow, channelLogin));
    }

    public void incrementUses(String channelLogin, String name) {
        database.useConnection(connection -> Jdbc.update(connection,
                "UPDATE twitch_custom_commands SET uses = uses + 1 WHERE channel_login = ? AND name = ?",
                channelLogin, normalize(name)));
    }

    private static String normalize(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private static CustomCommandRecord mapRow(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        return new CustomCommandRecord(
                resultSet.getString("name"),
                resultSet.getString("response"),
                resultSet.getString("created_by"),
                resultSet.getLong("created_at"),
                resultSet.getLong("uses"));
    }

    public record CustomCommandRecord(String name, String response, String createdBy, long createdAtEpochMillis, long uses) {
    }
}
