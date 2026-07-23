package de.destenylp.xBotenyy.twitchbot.persistence;

import de.destenylp.xBotenyy.common.persistence.sql.AbstractSqlManager;
import de.destenylp.xBotenyy.common.persistence.sql.Database;
import de.destenylp.xBotenyy.common.persistence.sql.Jdbc;

import java.time.Instant;
import java.util.List;

public class TwitchWatchtimeRepository extends AbstractSqlManager {
    public TwitchWatchtimeRepository(Database database) {
        super(database);
    }

    public void addSeconds(String channelLogin, String userId, String userLogin, long seconds) {
        long now = Instant.now().toEpochMilli();
        database.useConnection(connection -> Jdbc.update(connection,
                "INSERT INTO twitch_watchtime (channel_login, user_id, user_login, seconds, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?) "
                        + "ON CONFLICT(channel_login, user_id) DO UPDATE SET "
                        + "seconds = seconds + excluded.seconds, user_login = excluded.user_login, updated_at = excluded.updated_at",
                channelLogin, userId, userLogin, seconds, now));
    }

    public long getSeconds(String channelLogin, String userId) {
        return database.withConnection(connection -> Jdbc.queryOne(connection,
                        "SELECT seconds FROM twitch_watchtime WHERE channel_login = ? AND user_id = ?",
                        resultSet -> resultSet.getLong("seconds"), channelLogin, userId))
                .orElse(0L);
    }

    public List<WatchtimeRecord> top(String channelLogin, int limit) {
        return database.withConnection(connection -> Jdbc.query(connection,
                "SELECT user_id, user_login, seconds FROM twitch_watchtime WHERE channel_login = ? "
                        + "ORDER BY seconds DESC LIMIT ?",
                TwitchWatchtimeRepository::mapRow, channelLogin, limit));
    }

    private static WatchtimeRecord mapRow(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        return new WatchtimeRecord(
                resultSet.getString("user_id"),
                resultSet.getString("user_login"),
                resultSet.getLong("seconds"));
    }

    public record WatchtimeRecord(String userId, String userLogin, long seconds) {
    }
}
