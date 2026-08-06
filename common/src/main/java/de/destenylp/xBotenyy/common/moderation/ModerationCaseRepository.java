package de.destenylp.xBotenyy.common.moderation;
import de.destenylp.xBotenyy.common.persistence.sql.Database;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
public class ModerationCaseRepository {
    private final Database database;
    public ModerationCaseRepository(Database database) {
        this.database = database;
    }
    public long insert(ModerationPlatform platform, String scopeId, String targetId, String targetName,
                        String moderatorId, String moderatorName, ModerationAction action, String reason,
                        long durationSeconds, boolean synced) {
        return database.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO moderation_cases (platform, scope_id, target_id, target_name, moderator_id, "
                            + "moderator_name, action, reason, duration_seconds, created_at, active, synced) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, platform.name());
                statement.setString(2, scopeId);
                statement.setString(3, targetId);
                statement.setString(4, targetName);
                statement.setString(5, moderatorId);
                statement.setString(6, moderatorName);
                statement.setString(7, action.name());
                statement.setString(8, reason);
                statement.setLong(9, durationSeconds);
                statement.setLong(10, Instant.now().toEpochMilli());
                statement.setInt(11, synced ? 1 : 0);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    return keys.next() ? keys.getLong(1) : -1L;
                }
            }
        });
    }
    public void deactivate(ModerationPlatform platform, String scopeId, String targetId, ModerationAction action) {
        database.useConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE moderation_cases SET active = 0 WHERE platform = ? AND scope_id = ? AND target_id = ? "
                            + "AND action = ? AND active = 1")) {
                statement.setString(1, platform.name());
                statement.setString(2, scopeId);
                statement.setString(3, targetId);
                statement.setString(4, action.name());
                statement.executeUpdate();
            }
        });
    }
    public List<ModerationCase> findByTarget(ModerationPlatform platform, String scopeId, String targetId, int limit) {
        return database.withConnection(connection -> {
            List<ModerationCase> cases = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM moderation_cases WHERE platform = ? AND scope_id = ? AND target_id = ? "
                            + "ORDER BY created_at DESC LIMIT ?")) {
                statement.setString(1, platform.name());
                statement.setString(2, scopeId);
                statement.setString(3, targetId);
                statement.setInt(4, limit);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        cases.add(map(resultSet));
                    }
                }
            }
            return cases;
        });
    }
    public int countActiveWarnings(ModerationPlatform platform, String scopeId, String targetId) {
        return database.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM moderation_cases WHERE platform = ? AND scope_id = ? AND target_id = ? "
                            + "AND action = 'WARN' AND active = 1")) {
                statement.setString(1, platform.name());
                statement.setString(2, scopeId);
                statement.setString(3, targetId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? resultSet.getInt(1) : 0;
                }
            }
        });
    }
    private ModerationCase map(ResultSet resultSet) throws SQLException {
        return new ModerationCase(
                resultSet.getLong("id"),
                ModerationPlatform.valueOf(resultSet.getString("platform")),
                resultSet.getString("scope_id"),
                resultSet.getString("target_id"),
                resultSet.getString("target_name"),
                resultSet.getString("moderator_id"),
                resultSet.getString("moderator_name"),
                ModerationAction.valueOf(resultSet.getString("action")),
                resultSet.getString("reason"),
                resultSet.getLong("duration_seconds"),
                resultSet.getLong("created_at"),
                resultSet.getInt("active") == 1,
                resultSet.getInt("synced") == 1);
    }
}
