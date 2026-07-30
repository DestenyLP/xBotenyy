package de.destenylp.xBotenyy.common.moderation;

import de.destenylp.xBotenyy.common.persistence.sql.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

public class PendingLinkVerificationRepository {
    private final Database database;

    public PendingLinkVerificationRepository(Database database) {
        this.database = database;
    }

    public void save(PendingLinkVerification pending) {
        database.useConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO pending_link_verifications (code, discord_user_id, discord_username, "
                            + "twitch_user_id, twitch_login, initiated_from, expires_at) VALUES (?, ?, ?, ?, ?, ?, ?) "
                            + "ON CONFLICT(code) DO NOTHING")) {
                statement.setString(1, pending.code());
                statement.setString(2, pending.discordUserId());
                statement.setString(3, pending.discordUsername());
                statement.setString(4, pending.twitchUserId());
                statement.setString(5, pending.twitchLogin());
                statement.setString(6, pending.initiatedFrom().name());
                statement.setLong(7, pending.expiresAt());
                statement.executeUpdate();
            }
        });
    }

    public Optional<PendingLinkVerification> consume(String code) {
        return database.inTransaction(connection -> {
            try (PreparedStatement select = connection.prepareStatement(
                    "SELECT * FROM pending_link_verifications WHERE code = ?")) {
                select.setString(1, code);
                try (ResultSet resultSet = select.executeQuery()) {
                    if (!resultSet.next()) {
                        return Optional.<PendingLinkVerification>empty();
                    }
                    PendingLinkVerification pending = map(resultSet);
                    try (PreparedStatement delete = connection.prepareStatement(
                            "DELETE FROM pending_link_verifications WHERE code = ?")) {
                        delete.setString(1, code);
                        delete.executeUpdate();
                    }
                    if (pending.expiresAt() < Instant.now().toEpochMilli()) {
                        return Optional.<PendingLinkVerification>empty();
                    }
                    return Optional.of(pending);
                }
            }
        });
    }

    public int purgeExpired() {
        return database.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM pending_link_verifications WHERE expires_at < ?")) {
                statement.setLong(1, Instant.now().toEpochMilli());
                return statement.executeUpdate();
            }
        });
    }

    private PendingLinkVerification map(ResultSet resultSet) throws SQLException {
        return new PendingLinkVerification(
                resultSet.getString("code"),
                resultSet.getString("discord_user_id"),
                resultSet.getString("discord_username"),
                resultSet.getString("twitch_user_id"),
                resultSet.getString("twitch_login"),
                ModerationPlatform.valueOf(resultSet.getString("initiated_from")),
                resultSet.getLong("expires_at"));
    }
}
