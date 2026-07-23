package de.destenylp.xBotenyy.twitchbot.persistence;

import de.destenylp.xBotenyy.common.persistence.sql.AbstractSqlManager;
import de.destenylp.xBotenyy.common.persistence.sql.Database;
import de.destenylp.xBotenyy.common.persistence.sql.Jdbc;
import de.destenylp.xBotenyy.twitchbot.broadcast.TwitchBroadcastMessage;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class TwitchBroadcastRepository extends AbstractSqlManager {
    public TwitchBroadcastRepository(Database database) {
        super(database);
    }

    public TwitchBroadcastMessage add(String channelLogin, String message, long intervalSeconds,
                                       int minMessages, String createdBy) {
        long now = Instant.now().toEpochMilli();
        long id = database.withConnection(connection -> {
            Jdbc.update(connection,
                    "INSERT INTO twitch_broadcasts (channel_login, message, interval_seconds, min_messages, "
                            + "enabled, created_by, created_at, updated_at, last_sent_at) VALUES (?, ?, ?, ?, 1, ?, ?, ?, 0)",
                    channelLogin, message, intervalSeconds, minMessages, createdBy, now, now);
            return Jdbc.queryLong(connection, "SELECT last_insert_rowid()", 0);
        });
        return find(channelLogin, id).orElseThrow();
    }

    public boolean remove(String channelLogin, long id) {
        return database.withConnection(connection -> Jdbc.update(connection,
                "DELETE FROM twitch_broadcasts WHERE channel_login = ? AND id = ?", channelLogin, id)) > 0;
    }

    public boolean setEnabled(String channelLogin, long id, boolean enabled) {
        return database.withConnection(connection -> Jdbc.update(connection,
                "UPDATE twitch_broadcasts SET enabled = ?, updated_at = ? WHERE channel_login = ? AND id = ?",
                enabled, Instant.now().toEpochMilli(), channelLogin, id)) > 0;
    }

    public void markSent(String channelLogin, long id) {
        database.useConnection(connection -> Jdbc.update(connection,
                "UPDATE twitch_broadcasts SET last_sent_at = ? WHERE channel_login = ? AND id = ?",
                Instant.now().toEpochMilli(), channelLogin, id));
    }

    public Optional<TwitchBroadcastMessage> find(String channelLogin, long id) {
        return database.withConnection(connection -> Jdbc.queryOne(connection,
                "SELECT * FROM twitch_broadcasts WHERE channel_login = ? AND id = ?",
                TwitchBroadcastRepository::mapRow, channelLogin, id));
    }

    public List<TwitchBroadcastMessage> list(String channelLogin) {
        return database.withConnection(connection -> Jdbc.query(connection,
                "SELECT * FROM twitch_broadcasts WHERE channel_login = ? ORDER BY id ASC",
                TwitchBroadcastRepository::mapRow, channelLogin));
    }

    public List<TwitchBroadcastMessage> listEnabled(String channelLogin) {
        return database.withConnection(connection -> Jdbc.query(connection,
                "SELECT * FROM twitch_broadcasts WHERE channel_login = ? AND enabled = 1 ORDER BY id ASC",
                TwitchBroadcastRepository::mapRow, channelLogin));
    }

    private static TwitchBroadcastMessage mapRow(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        return new TwitchBroadcastMessage(
                resultSet.getLong("id"),
                resultSet.getString("channel_login"),
                resultSet.getString("message"),
                resultSet.getLong("interval_seconds"),
                resultSet.getInt("min_messages"),
                Jdbc.getBoolean(resultSet, "enabled"),
                resultSet.getString("created_by"),
                resultSet.getLong("created_at"),
                resultSet.getLong("last_sent_at"));
    }
}
