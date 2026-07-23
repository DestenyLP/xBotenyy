package de.destenylp.xBotenyy.discordbot.giveaways;

import de.destenylp.xBotenyy.common.persistence.sql.AbstractSqlManager;
import de.destenylp.xBotenyy.common.persistence.sql.Database;
import de.destenylp.xBotenyy.common.persistence.sql.Jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GiveawayManager extends AbstractSqlManager implements GiveawayRepository {
    public GiveawayManager(Database database) {
        super(database);
    }

    @Override
    public Giveaway createGiveaway(Giveaway draft) {
        return database.inTransaction(connection -> {
            ensureSettingsRow(connection, "giveaway_guild_settings", draft.getGuildId());
            String id = nextSequentialId(connection, "giveaway_guild_settings", "giveaway_counter", draft.getGuildId());
            draft.assignId(id);
            Jdbc.update(connection, """
                    INSERT INTO giveaways (guild_id, giveaway_id, channel_id, message_id, prize, description,
                        winner_count, host_id, host_name, required_role_id, status, created_at, end_at, ended_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    draft.getGuildId(), draft.getId(), draft.getChannelId(), draft.getMessageId(), draft.getPrize(),
                    draft.getDescription(), draft.getWinnerCount(), draft.getHostId(), draft.getHostName(),
                    draft.getRequiredRoleId(), draft.getStatus(), draft.getCreatedAt(), draft.getEndAt(),
                    draft.getEndedAt());
            return draft;
        });
    }

    @Override
    public void save(Giveaway giveaway) {
        database.runInTransaction(connection -> {
            Jdbc.update(connection, """
                    UPDATE giveaways SET channel_id = ?, message_id = ?, status = ?, ended_at = ?
                    WHERE guild_id = ? AND giveaway_id = ?
                    """, giveaway.getChannelId(), giveaway.getMessageId(), giveaway.getStatus(),
                    giveaway.getEndedAt(), giveaway.getGuildId(), giveaway.getId());
            Jdbc.update(connection, "DELETE FROM giveaway_winners WHERE guild_id = ? AND giveaway_id = ?",
                    giveaway.getGuildId(), giveaway.getId());
            List<String> winners = giveaway.getWinnerIds();
            for (int position = 0; position < winners.size(); position++) {
                Jdbc.update(connection, """
                        INSERT INTO giveaway_winners (guild_id, giveaway_id, member_id, position)
                        VALUES (?, ?, ?, ?)
                        """, giveaway.getGuildId(), giveaway.getId(), winners.get(position), position);
            }
        });
    }

    @Override
    public void persistParticipant(String guildId, String giveawayId, String memberId, boolean joined) {
        database.runInTransaction(connection -> {
            if (joined) {
                Jdbc.update(connection, """
                        INSERT OR IGNORE INTO giveaway_participants (guild_id, giveaway_id, member_id)
                        VALUES (?, ?, ?)
                        """, guildId, giveawayId, memberId);
            } else {
                Jdbc.update(connection,
                        "DELETE FROM giveaway_participants WHERE guild_id = ? AND giveaway_id = ? AND member_id = ?",
                        guildId, giveawayId, memberId);
            }
        });
    }

    @Override
    public void attachMessage(String guildId, String giveawayId, String channelId, String messageId) {
        database.runInTransaction(connection -> Jdbc.update(connection,
                "UPDATE giveaways SET channel_id = ?, message_id = ? WHERE guild_id = ? AND giveaway_id = ?",
                channelId, messageId, guildId, giveawayId));
    }

    @Override
    public Optional<Giveaway> getGiveaway(String guildId, String id) {
        return database.withConnection(connection -> Jdbc.queryOne(connection,
                "SELECT * FROM giveaways WHERE guild_id = ? AND giveaway_id = ? COLLATE NOCASE",
                this::mapGiveaway, guildId, id));
    }

    @Override
    public Optional<Giveaway> getGiveawayByMessage(String guildId, String messageId) {
        if (messageId == null) {
            return Optional.empty();
        }
        return database.withConnection(connection -> Jdbc.queryOne(connection,
                "SELECT * FROM giveaways WHERE guild_id = ? AND message_id = ?", this::mapGiveaway, guildId, messageId));
    }

    @Override
    public List<Giveaway> getGiveaways(String guildId) {
        return database.withConnection(connection -> Jdbc.query(connection,
                "SELECT * FROM giveaways WHERE guild_id = ? ORDER BY created_at", this::mapGiveaway, guildId));
    }

    @Override
    public List<Giveaway> getRunningGiveaways(String guildId) {
        return database.withConnection(connection -> Jdbc.query(connection,
                "SELECT * FROM giveaways WHERE guild_id = ? AND status = ? ORDER BY end_at",
                this::mapGiveaway, guildId, GiveawayStatus.RUNNING));
    }

    @Override
    public Map<String, List<Giveaway>> getAllRunningGiveawaysByGuild() {
        List<Giveaway> running = database.withConnection(connection -> Jdbc.query(connection,
                "SELECT * FROM giveaways WHERE status = ? ORDER BY guild_id, end_at",
                this::mapGiveaway, GiveawayStatus.RUNNING));
        Map<String, List<Giveaway>> result = new ConcurrentHashMap<>();
        for (Giveaway giveaway : running) {
            result.computeIfAbsent(giveaway.getGuildId(), key -> new ArrayList<>()).add(giveaway);
        }
        return result;
    }

    @Override
    public int pruneFinishedGiveaways(Duration retention) {
        long threshold = Instant.now().minus(retention).toEpochMilli();
        return database.withConnection(connection -> Jdbc.update(connection,
                "DELETE FROM giveaways WHERE status != ? AND ended_at > 0 AND ended_at < ?",
                GiveawayStatus.RUNNING, threshold));
    }

    private Giveaway mapGiveaway(ResultSet resultSet) throws SQLException {
        String guildId = resultSet.getString("guild_id");
        String giveawayId = resultSet.getString("giveaway_id");
        Connection connection = resultSet.getStatement().getConnection();
        Set<String> participants = new LinkedHashSet<>(Jdbc.query(connection,
                "SELECT member_id FROM giveaway_participants WHERE guild_id = ? AND giveaway_id = ?",
                rs -> rs.getString("member_id"), guildId, giveawayId));
        List<String> winners = Jdbc.query(connection,
                "SELECT member_id FROM giveaway_winners WHERE guild_id = ? AND giveaway_id = ? ORDER BY position",
                rs -> rs.getString("member_id"), guildId, giveawayId);
        return new Giveaway(giveawayId, guildId, resultSet.getString("channel_id"), resultSet.getString("message_id"),
                resultSet.getString("prize"), resultSet.getString("description"), resultSet.getInt("winner_count"),
                resultSet.getString("host_id"), resultSet.getString("host_name"),
                resultSet.getString("required_role_id"), GiveawayStatus.valueOf(resultSet.getString("status")),
                resultSet.getLong("created_at"), resultSet.getLong("end_at"), resultSet.getLong("ended_at"),
                participants, winners);
    }
}
