package de.destenylp.xBotenyy.twitchbot.persistence;

import de.destenylp.xBotenyy.common.core.Prunable;
import de.destenylp.xBotenyy.common.persistence.sql.AbstractSqlManager;
import de.destenylp.xBotenyy.common.persistence.sql.Database;
import de.destenylp.xBotenyy.common.persistence.sql.Jdbc;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class TwitchEventLogRepository extends AbstractSqlManager implements Prunable {
    public TwitchEventLogRepository(Database database) {
        super(database);
    }

    public void insert(String channelLogin, String eventType, String actorId, String detail) {
        database.useConnection(connection -> Jdbc.update(connection,
                "INSERT INTO twitch_event_log (channel_login, event_type, actor_id, detail, created_at) "
                        + "VALUES (?, ?, ?, ?, ?)",
                channelLogin, eventType, actorId, detail, Instant.now().toEpochMilli()));
    }

    public List<TwitchEventLogEntry> recent(String channelLogin, int limit) {
        return database.withConnection(connection -> Jdbc.query(connection,
                "SELECT * FROM twitch_event_log WHERE channel_login = ? ORDER BY id DESC LIMIT ?",
                TwitchEventLogRepository::mapRow, channelLogin, limit));
    }

    @Override
    public int pruneOldEntries(Duration retention) {
        long threshold = Instant.now().minus(retention).toEpochMilli();
        return database.withConnection(connection -> Jdbc.update(connection,
                "DELETE FROM twitch_event_log WHERE created_at < ?", threshold));
    }

    private static TwitchEventLogEntry mapRow(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        return new TwitchEventLogEntry(
                resultSet.getLong("id"),
                resultSet.getString("channel_login"),
                resultSet.getString("event_type"),
                resultSet.getString("actor_id"),
                resultSet.getString("detail"),
                resultSet.getLong("created_at"));
    }

    public record TwitchEventLogEntry(long id, String channelLogin, String eventType, String actorId,
                                       String detail, long createdAtEpochMillis) {
    }
}
