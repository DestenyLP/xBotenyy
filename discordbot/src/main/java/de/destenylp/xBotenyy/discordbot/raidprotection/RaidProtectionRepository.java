package de.destenylp.xBotenyy.discordbot.raidprotection;

import de.destenylp.xBotenyy.common.persistence.sql.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RaidProtectionRepository {
    private final Database database;

    public RaidProtectionRepository(Database database) {
        this.database = database;
    }

    public RaidProtectionSettings getOrEmpty(String guildId) {
        return database.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM raid_protection_settings WHERE guild_id = ?")) {
                statement.setString(1, guildId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? map(resultSet) : RaidProtectionSettings.empty(guildId);
                }
            }
        });
    }

    public void setEnabled(String guildId, boolean enabled) {
        RaidProtectionSettings current = getOrEmpty(guildId);
        upsert(new RaidProtectionSettings(guildId, enabled, current.alertChannelId()));
    }

    public void setAlertChannel(String guildId, String channelId) {
        RaidProtectionSettings current = getOrEmpty(guildId);
        upsert(new RaidProtectionSettings(guildId, current.enabled(), channelId));
    }

    private void upsert(RaidProtectionSettings settings) {
        database.useConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO raid_protection_settings (guild_id, enabled, alert_channel_id) VALUES (?, ?, ?) "
                            + "ON CONFLICT(guild_id) DO UPDATE SET enabled = excluded.enabled, "
                            + "alert_channel_id = excluded.alert_channel_id")) {
                statement.setString(1, settings.guildId());
                statement.setInt(2, settings.enabled() ? 1 : 0);
                statement.setString(3, settings.alertChannelId());
                statement.executeUpdate();
            }
        });
    }

    private RaidProtectionSettings map(ResultSet resultSet) throws SQLException {
        return new RaidProtectionSettings(
                resultSet.getString("guild_id"),
                resultSet.getInt("enabled") == 1,
                resultSet.getString("alert_channel_id"));
    }
}
