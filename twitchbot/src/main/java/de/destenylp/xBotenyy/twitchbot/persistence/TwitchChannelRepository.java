package de.destenylp.xBotenyy.twitchbot.persistence;

import de.destenylp.xBotenyy.common.persistence.sql.AbstractSqlManager;
import de.destenylp.xBotenyy.common.persistence.sql.Database;
import de.destenylp.xBotenyy.common.persistence.sql.Jdbc;

import java.time.Instant;

public class TwitchChannelRepository extends AbstractSqlManager {
    public TwitchChannelRepository(Database database) {
        super(database);
    }

    public void recordJoin(String channelLogin) {
        long now = Instant.now().toEpochMilli();
        database.useConnection(connection -> Jdbc.update(connection,
                "INSERT INTO twitch_channels (channel_login, joined_at, last_seen_at) VALUES (?, ?, ?) "
                        + "ON CONFLICT(channel_login) DO UPDATE SET last_seen_at = excluded.last_seen_at",
                channelLogin, now, now));
    }

    public void recordActivity(String channelLogin) {
        long now = Instant.now().toEpochMilli();
        database.useConnection(connection -> Jdbc.update(connection,
                "UPDATE twitch_channels SET last_seen_at = ? WHERE channel_login = ?", now, channelLogin));
    }
}
