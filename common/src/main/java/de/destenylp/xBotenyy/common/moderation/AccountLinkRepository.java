package de.destenylp.xBotenyy.common.moderation;

import de.destenylp.xBotenyy.common.persistence.sql.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

public class AccountLinkRepository {
    private final Database database;

    public AccountLinkRepository(Database database) {
        this.database = database;
    }

    public void save(String discordUserId, String twitchUserId, String twitchLogin) {
        database.useConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO account_links (discord_user_id, twitch_user_id, twitch_login, linked_at) "
                            + "VALUES (?, ?, ?, ?) "
                            + "ON CONFLICT(discord_user_id) DO UPDATE SET twitch_user_id = excluded.twitch_user_id, "
                            + "twitch_login = excluded.twitch_login, linked_at = excluded.linked_at")) {
                statement.setString(1, discordUserId);
                statement.setString(2, twitchUserId);
                statement.setString(3, twitchLogin);
                statement.setLong(4, Instant.now().toEpochMilli());
                statement.executeUpdate();
            }
        });
    }

    public Optional<AccountLink> findByDiscordUserId(String discordUserId) {
        return database.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM account_links WHERE discord_user_id = ?")) {
                statement.setString(1, discordUserId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.of(map(resultSet)) : Optional.<AccountLink>empty();
                }
            }
        });
    }

    public Optional<AccountLink> findByTwitchUserId(String twitchUserId) {
        return database.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM account_links WHERE twitch_user_id = ?")) {
                statement.setString(1, twitchUserId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.of(map(resultSet)) : Optional.<AccountLink>empty();
                }
            }
        });
    }

    public java.util.List<AccountLink> findAll() {
        return database.withConnection(connection -> {
            java.util.List<AccountLink> links = new java.util.ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM account_links");
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    links.add(map(resultSet));
                }
            }
            return links;
        });
    }

    public void delete(String discordUserId) {
        database.useConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM account_links WHERE discord_user_id = ?")) {
                statement.setString(1, discordUserId);
                statement.executeUpdate();
            }
        });
    }

    private AccountLink map(ResultSet resultSet) throws SQLException {
        return new AccountLink(
                resultSet.getString("discord_user_id"),
                resultSet.getString("twitch_user_id"),
                resultSet.getString("twitch_login"),
                resultSet.getLong("linked_at"));
    }
}

