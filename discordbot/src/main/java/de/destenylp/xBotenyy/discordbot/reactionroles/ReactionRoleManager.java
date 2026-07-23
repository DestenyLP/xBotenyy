package de.destenylp.xBotenyy.discordbot.reactionroles;

import de.destenylp.xBotenyy.common.persistence.sql.AbstractSqlManager;
import de.destenylp.xBotenyy.common.persistence.sql.Database;
import de.destenylp.xBotenyy.common.persistence.sql.Jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ReactionRoleManager extends AbstractSqlManager implements ReactionRoleRepository {
    public ReactionRoleManager(Database database) {
        super(database);
    }

    @Override
    public ReactionRoleMessage createMessage(String guildId, String channelId, String messageId) {
        database.runInTransaction(connection -> Jdbc.update(connection, """
                INSERT INTO reactionrole_messages (guild_id, channel_id, message_id) VALUES (?, ?, ?)
                """, guildId, channelId, messageId));
        return new ReactionRoleMessage(guildId, channelId, messageId);
    }

    @Override
    public ReactionRoleMessage getOrCreateMessage(String guildId, String channelId, String messageId) {
        return getMessage(guildId, messageId).orElseGet(() -> createMessage(guildId, channelId, messageId));
    }

    @Override
    public Optional<ReactionRoleMessage> getMessage(String guildId, String messageId) {
        return database.withConnection(connection -> Jdbc.queryOne(connection,
                "SELECT * FROM reactionrole_messages WHERE guild_id = ? AND message_id = ?",
                this::mapMessage, guildId, messageId));
    }

    @Override
    public List<ReactionRoleMessage> getMessages(String guildId) {
        return database.withConnection(connection -> Jdbc.query(connection,
                "SELECT * FROM reactionrole_messages WHERE guild_id = ?", this::mapMessage, guildId));
    }

    @Override
    public boolean addEntry(String guildId, String messageId, ReactionRoleEntry entry) {
        if (getMessage(guildId, messageId).isEmpty()) {
            return false;
        }
        database.runInTransaction(connection -> Jdbc.update(connection, """
                INSERT INTO reactionrole_entries (message_id, component_id, role_id, type, emoji, button_label,
                    button_style)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, messageId, componentKey(entry), entry.getRoleId(), entry.getType(), entry.getEmoji(),
                entry.getButtonLabel(), entry.getButtonStyle()));
        return true;
    }

    @Override
    public boolean removeEntry(String guildId, String messageId, String identifier) {
        if (getMessage(guildId, messageId).isEmpty()) {
            return false;
        }
        int removed = database.withConnection(connection -> Jdbc.update(connection,
                "DELETE FROM reactionrole_entries WHERE message_id = ? AND (role_id = ? OR emoji = ?)",
                messageId, identifier, identifier));
        return removed > 0;
    }

    private String componentKey(ReactionRoleEntry entry) {
        return entry.getComponentId() != null ? entry.getComponentId() : "reaction:" + entry.getRoleId();
    }

    private ReactionRoleMessage mapMessage(ResultSet resultSet) throws SQLException {
        String guildId = resultSet.getString("guild_id");
        String channelId = resultSet.getString("channel_id");
        String messageId = resultSet.getString("message_id");
        ReactionRoleMessage message = new ReactionRoleMessage(guildId, channelId, messageId);
        Connection connection = resultSet.getStatement().getConnection();
        List<ReactionRoleEntry> entries = Jdbc.query(connection,
                "SELECT * FROM reactionrole_entries WHERE message_id = ?", this::mapEntry, messageId);
        entries.forEach(message::addEntry);
        return message;
    }

    private ReactionRoleEntry mapEntry(ResultSet resultSet) throws SQLException {
        String type = resultSet.getString("type");
        return ReactionRoleEntry.builder()
                .componentId(resultSet.getString("component_id"))
                .roleId(resultSet.getString("role_id"))
                .type(ReactionRoleType.valueOf(type))
                .emoji(resultSet.getString("emoji"))
                .buttonLabel(resultSet.getString("button_label"))
                .buttonStyle(resultSet.getString("button_style"))
                .build();
    }
}
