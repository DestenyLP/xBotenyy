package de.destenylp.xBotenyy.common.persistence.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

public abstract class AbstractSqlManager {
    protected final Database database;

    protected AbstractSqlManager(Database database) {
        this.database = database;
    }

    protected static void ensureSettingsRow(Connection connection, String table, String guildId) throws SQLException {
        Jdbc.update(connection,
                "INSERT INTO " + table + " (guild_id) VALUES (?) ON CONFLICT(guild_id) DO NOTHING", guildId);
    }

    protected static String nextSequentialId(Connection connection, String table, String counterColumn, String guildId)
            throws SQLException {
        Jdbc.update(connection,
                "UPDATE " + table + " SET " + counterColumn + " = " + counterColumn + " + 1 WHERE guild_id = ?",
                guildId);
        long number = Jdbc.queryLong(connection,
                "SELECT " + counterColumn + " FROM " + table + " WHERE guild_id = ?", 0, guildId);
        return String.format("%04d", number);
    }

    protected static String generateUniqueShortId(Connection connection, String table, String idColumn,
            String guildIdColumn, String guildId, boolean uppercase) throws SQLException {
        String candidate;
        do {
            candidate = UUID.randomUUID().toString().substring(0, 6);
            if (uppercase) {
                candidate = candidate.toUpperCase();
            }
        } while (Jdbc.queryLong(connection,
                "SELECT COUNT(*) FROM " + table + " WHERE " + guildIdColumn + " = ? AND " + idColumn
                        + " = ? COLLATE NOCASE",
                0, guildId, candidate) > 0);
        return candidate;
    }
}
