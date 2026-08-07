package de.destenylp.xBotenyy.discordbot.moderation;

import de.destenylp.xBotenyy.common.moderation.TwitchRoleSyncStatus;
import de.destenylp.xBotenyy.common.persistence.sql.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class ModerationRoleSettingsRepository {
    private final Database database;

    public ModerationRoleSettingsRepository(Database database) {
        this.database = database;
    }

    public ModerationRoleSettings getOrEmpty(String guildId) {
        return find(guildId).orElseGet(() -> ModerationRoleSettings.empty(guildId));
    }

    public Optional<ModerationRoleSettings> find(String guildId) {
        return database.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM moderation_role_settings WHERE guild_id = ?")) {
                statement.setString(1, guildId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.of(map(resultSet)) : Optional.<ModerationRoleSettings>empty();
                }
            }
        });
    }

    public void setWarnRole(String guildId, String roleId) {
        upsert(guildId, settings -> new ModerationRoleSettings(guildId, roleId, settings.muteRoleId(),
                settings.banRoleId(), settings.moderatorRoleIds(), settings.adminRoleIds(),
                settings.syncSubscriberRoleId(), settings.syncVipRoleId(), settings.syncModeratorRoleId(),
                settings.syncBroadcasterRoleId()));
    }

    public void setMuteRole(String guildId, String roleId) {
        upsert(guildId, settings -> new ModerationRoleSettings(guildId, settings.warnRoleId(), roleId,
                settings.banRoleId(), settings.moderatorRoleIds(), settings.adminRoleIds(),
                settings.syncSubscriberRoleId(), settings.syncVipRoleId(), settings.syncModeratorRoleId(),
                settings.syncBroadcasterRoleId()));
    }

    public void setBanRole(String guildId, String roleId) {
        upsert(guildId, settings -> new ModerationRoleSettings(guildId, settings.warnRoleId(), settings.muteRoleId(),
                roleId, settings.moderatorRoleIds(), settings.adminRoleIds(), settings.syncSubscriberRoleId(),
                settings.syncVipRoleId(), settings.syncModeratorRoleId(), settings.syncBroadcasterRoleId()));
    }

    public void addModeratorRole(String guildId, String roleId) {
        upsert(guildId, settings -> {
            List<String> updated = new java.util.ArrayList<>(settings.moderatorRoleIds());
            if (!updated.contains(roleId)) {
                updated.add(roleId);
            }
            return new ModerationRoleSettings(guildId, settings.warnRoleId(), settings.muteRoleId(),
                    settings.banRoleId(), updated, settings.adminRoleIds(), settings.syncSubscriberRoleId(),
                    settings.syncVipRoleId(), settings.syncModeratorRoleId(), settings.syncBroadcasterRoleId());
        });
    }

    public void removeModeratorRole(String guildId, String roleId) {
        upsert(guildId, settings -> {
            List<String> updated = new java.util.ArrayList<>(settings.moderatorRoleIds());
            updated.remove(roleId);
            return new ModerationRoleSettings(guildId, settings.warnRoleId(), settings.muteRoleId(),
                    settings.banRoleId(), updated, settings.adminRoleIds(), settings.syncSubscriberRoleId(),
                    settings.syncVipRoleId(), settings.syncModeratorRoleId(), settings.syncBroadcasterRoleId());
        });
    }

    public void addAdminRole(String guildId, String roleId) {
        upsert(guildId, settings -> {
            List<String> updated = new java.util.ArrayList<>(settings.adminRoleIds());
            if (!updated.contains(roleId)) {
                updated.add(roleId);
            }
            return new ModerationRoleSettings(guildId, settings.warnRoleId(), settings.muteRoleId(),
                    settings.banRoleId(), settings.moderatorRoleIds(), updated, settings.syncSubscriberRoleId(),
                    settings.syncVipRoleId(), settings.syncModeratorRoleId(), settings.syncBroadcasterRoleId());
        });
    }

    public void removeAdminRole(String guildId, String roleId) {
        upsert(guildId, settings -> {
            List<String> updated = new java.util.ArrayList<>(settings.adminRoleIds());
            updated.remove(roleId);
            return new ModerationRoleSettings(guildId, settings.warnRoleId(), settings.muteRoleId(),
                    settings.banRoleId(), settings.moderatorRoleIds(), updated, settings.syncSubscriberRoleId(),
                    settings.syncVipRoleId(), settings.syncModeratorRoleId(), settings.syncBroadcasterRoleId());
        });
    }

    public void setSyncRole(String guildId, TwitchRoleSyncStatus status, String roleId) {
        upsert(guildId, settings -> switch (status) {
            case SUBSCRIBER -> new ModerationRoleSettings(guildId, settings.warnRoleId(), settings.muteRoleId(),
                    settings.banRoleId(), settings.moderatorRoleIds(), settings.adminRoleIds(), roleId,
                    settings.syncVipRoleId(), settings.syncModeratorRoleId(), settings.syncBroadcasterRoleId());
            case VIP -> new ModerationRoleSettings(guildId, settings.warnRoleId(), settings.muteRoleId(),
                    settings.banRoleId(), settings.moderatorRoleIds(), settings.adminRoleIds(),
                    settings.syncSubscriberRoleId(), roleId, settings.syncModeratorRoleId(),
                    settings.syncBroadcasterRoleId());
            case MODERATOR -> new ModerationRoleSettings(guildId, settings.warnRoleId(), settings.muteRoleId(),
                    settings.banRoleId(), settings.moderatorRoleIds(), settings.adminRoleIds(),
                    settings.syncSubscriberRoleId(), settings.syncVipRoleId(), roleId, settings.syncBroadcasterRoleId());
            case BROADCASTER -> new ModerationRoleSettings(guildId, settings.warnRoleId(), settings.muteRoleId(),
                    settings.banRoleId(), settings.moderatorRoleIds(), settings.adminRoleIds(),
                    settings.syncSubscriberRoleId(), settings.syncVipRoleId(), settings.syncModeratorRoleId(), roleId);
        });
    }

    private void upsert(String guildId, Function<ModerationRoleSettings, ModerationRoleSettings> updater) {
        ModerationRoleSettings updated = updater.apply(getOrEmpty(guildId));
        database.useConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO moderation_role_settings (guild_id, warn_role_id, mute_role_id, ban_role_id, "
                            + "moderator_role_ids, admin_role_ids, sync_subscriber_role_id, sync_vip_role_id, "
                            + "sync_moderator_role_id, sync_broadcaster_role_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                            + "ON CONFLICT(guild_id) DO UPDATE SET warn_role_id = excluded.warn_role_id, "
                            + "mute_role_id = excluded.mute_role_id, ban_role_id = excluded.ban_role_id, "
                            + "moderator_role_ids = excluded.moderator_role_ids, admin_role_ids = excluded.admin_role_ids, "
                            + "sync_subscriber_role_id = excluded.sync_subscriber_role_id, "
                            + "sync_vip_role_id = excluded.sync_vip_role_id, "
                            + "sync_moderator_role_id = excluded.sync_moderator_role_id, "
                            + "sync_broadcaster_role_id = excluded.sync_broadcaster_role_id")) {
                statement.setString(1, guildId);
                statement.setString(2, updated.warnRoleId());
                statement.setString(3, updated.muteRoleId());
                statement.setString(4, updated.banRoleId());
                statement.setString(5, String.join(",", updated.moderatorRoleIds()));
                statement.setString(6, String.join(",", updated.adminRoleIds()));
                statement.setString(7, updated.syncSubscriberRoleId());
                statement.setString(8, updated.syncVipRoleId());
                statement.setString(9, updated.syncModeratorRoleId());
                statement.setString(10, updated.syncBroadcasterRoleId());
                statement.executeUpdate();
            }
        });
    }

    private ModerationRoleSettings map(ResultSet resultSet) throws SQLException {
        return new ModerationRoleSettings(
                resultSet.getString("guild_id"),
                resultSet.getString("warn_role_id"),
                resultSet.getString("mute_role_id"),
                resultSet.getString("ban_role_id"),
                splitRoles(resultSet.getString("moderator_role_ids")),
                splitRoles(resultSet.getString("admin_role_ids")),
                resultSet.getString("sync_subscriber_role_id"),
                resultSet.getString("sync_vip_role_id"),
                resultSet.getString("sync_moderator_role_id"),
                resultSet.getString("sync_broadcaster_role_id"));
    }

    private List<String> splitRoles(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(",")).filter(part -> !part.isBlank()).toList();
    }
}

