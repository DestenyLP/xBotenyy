package de.destenylp.xBotenyy.discordbot.tickets;

import de.destenylp.xBotenyy.common.persistence.sql.AbstractSqlManager;
import de.destenylp.xBotenyy.common.persistence.sql.Database;
import de.destenylp.xBotenyy.common.persistence.sql.Jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TicketManager extends AbstractSqlManager implements TicketRepository {
    private final int defaultMaxOpenTicketsPerMember;

    public TicketManager(Database database, int defaultMaxOpenTicketsPerMember) {
        super(database);
        this.defaultMaxOpenTicketsPerMember = Math.max(1, defaultMaxOpenTicketsPerMember);
    }

    @Override
    public TicketSettings getOrCreateSettings(String guildId) {
        return database.inTransaction(connection -> {
            ensureSettingsRow(connection, guildId);
            return readSettings(connection, guildId).orElseThrow();
        });
    }

    @Override
    public Optional<TicketSettings> getSettings(String guildId) {
        return database.withConnection(connection -> readSettings(connection, guildId));
    }

    private void ensureSettingsRow(Connection connection, String guildId) throws SQLException {
        Jdbc.update(connection, """
                INSERT INTO ticket_guild_settings (guild_id, max_open_tickets_per_member)
                VALUES (?, ?)
                ON CONFLICT(guild_id) DO NOTHING
                """, guildId, defaultMaxOpenTicketsPerMember);
    }

    private Optional<TicketSettings> readSettings(Connection connection, String guildId) throws SQLException {
        return Jdbc.queryOne(connection, "SELECT * FROM ticket_guild_settings WHERE guild_id = ?",
                resultSet -> mapSettings(guildId, resultSet), guildId);
    }

    private TicketSettings mapSettings(String guildId, ResultSet resultSet) throws SQLException {
        TicketSettings settings = new TicketSettings(() -> getTickets(guildId));
        settings.setCategoryChannelId(Jdbc.getString(resultSet, "category_channel_id"));
        settings.setSupportRoleId(Jdbc.getString(resultSet, "support_role_id"));
        settings.setLogChannelId(Jdbc.getString(resultSet, "log_channel_id"));
        settings.setTranscriptChannelId(Jdbc.getString(resultSet, "transcript_channel_id"));
        settings.setPanel(Jdbc.getString(resultSet, "panel_channel_id"), Jdbc.getString(resultSet, "panel_message_id"));
        settings.setMaxOpenTicketsPerMember(resultSet.getInt("max_open_tickets_per_member"));
        settings.setAutoCloseInactivityHours(resultSet.getInt("auto_close_inactivity_hours"));
        return settings;
    }

    @Override
    public void updateCategoryChannel(String guildId, String channelId) {
        upsertSettingsColumn(guildId, "category_channel_id", channelId);
    }

    @Override
    public void updateSupportRole(String guildId, String roleId) {
        upsertSettingsColumn(guildId, "support_role_id", roleId);
    }

    @Override
    public void updateLogChannel(String guildId, String channelId) {
        upsertSettingsColumn(guildId, "log_channel_id", channelId);
    }

    @Override
    public void updateTranscriptChannel(String guildId, String channelId) {
        upsertSettingsColumn(guildId, "transcript_channel_id", channelId);
    }

    @Override
    public void updateMaxOpenTicketsPerMember(String guildId, int max) {
        upsertSettingsColumn(guildId, "max_open_tickets_per_member", Math.max(1, max));
    }

    @Override
    public void updateAutoCloseInactivityHours(String guildId, int hours) {
        upsertSettingsColumn(guildId, "auto_close_inactivity_hours", Math.max(0, hours));
    }

    @Override
    public void updatePanel(String guildId, String channelId, String messageId) {
        database.runInTransaction(connection -> {
            ensureSettingsRow(connection, guildId);
            Jdbc.update(connection,
                    "UPDATE ticket_guild_settings SET panel_channel_id = ?, panel_message_id = ? WHERE guild_id = ?",
                    channelId, messageId, guildId);
        });
    }

    private void upsertSettingsColumn(String guildId, String column, Object value) {
        database.runInTransaction(connection -> {
            ensureSettingsRow(connection, guildId);
            Jdbc.update(connection, "UPDATE ticket_guild_settings SET " + column + " = ? WHERE guild_id = ?",
                    value, guildId);
        });
    }

    @Override
    public Ticket createTicket(Ticket draft) {
        return database.inTransaction(connection -> {
            ensureSettingsRow(connection, draft.getGuildId());
            String id = nextSequentialId(connection, "ticket_guild_settings", "ticket_counter", draft.getGuildId());
            draft.assignId(id);
            insertTicket(connection, draft);
            return draft;
        });
    }

    private void insertTicket(Connection connection, Ticket ticket) throws SQLException {
        Jdbc.update(connection, """
                INSERT INTO tickets (guild_id, ticket_id, channel_id, control_message_id, author_id, author_name,
                    category, priority, status, subject, description, created_at, updated_at, last_activity_at,
                    closed_at, claimed_by_id, claimed_by_name, closed_by_id, closed_by_name, close_reason,
                    transcript_file_name, log_channel_id, log_message_id, rating_score, rating_comment,
                    auto_close_warning_sent)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                ticket.getGuildId(), ticket.getId(), ticket.getChannelId(), ticket.getControlMessageId(),
                ticket.getAuthorId(), ticket.getAuthorName(), ticket.getCategory(), ticket.getPriority(),
                ticket.getStatus(), ticket.getSubject(), ticket.getDescription(), ticket.getCreatedAt(),
                ticket.getUpdatedAt(), ticket.getLastActivityAt(), ticket.getClosedAt(), ticket.getClaimedById(),
                ticket.getClaimedByName(), ticket.getClosedById(), ticket.getClosedByName(), ticket.getCloseReason(),
                ticket.getTranscriptFileName(), ticket.getLogChannelId(), ticket.getLogMessageId(),
                ticket.getRatingScore(), ticket.getRatingComment(), ticket.isAutoCloseWarningSent());
    }

    private void updateTicketRow(Connection connection, Ticket ticket) throws SQLException {
        Jdbc.update(connection, """
                UPDATE tickets SET channel_id = ?, control_message_id = ?, priority = ?, status = ?,
                    updated_at = ?, last_activity_at = ?, closed_at = ?, claimed_by_id = ?, claimed_by_name = ?,
                    closed_by_id = ?, closed_by_name = ?, close_reason = ?, transcript_file_name = ?,
                    log_channel_id = ?, log_message_id = ?, rating_score = ?, rating_comment = ?,
                    auto_close_warning_sent = ?
                WHERE guild_id = ? AND ticket_id = ?
                """,
                ticket.getChannelId(), ticket.getControlMessageId(), ticket.getPriority(), ticket.getStatus(),
                ticket.getUpdatedAt(), ticket.getLastActivityAt(), ticket.getClosedAt(), ticket.getClaimedById(),
                ticket.getClaimedByName(), ticket.getClosedById(), ticket.getClosedByName(), ticket.getCloseReason(),
                ticket.getTranscriptFileName(), ticket.getLogChannelId(), ticket.getLogMessageId(),
                ticket.getRatingScore(), ticket.getRatingComment(), ticket.isAutoCloseWarningSent(),
                ticket.getGuildId(), ticket.getId());
    }

    private void save(Ticket ticket) {
        database.runInTransaction(connection -> updateTicketRow(connection, ticket));
    }

    @Override
    public Optional<Ticket> getTicket(String guildId, String id) {
        return database.withConnection(connection -> Jdbc.queryOne(connection,
                "SELECT * FROM tickets WHERE guild_id = ? AND ticket_id = ? COLLATE NOCASE",
                this::mapTicket, guildId, id));
    }

    @Override
    public Optional<Ticket> getTicketByChannel(String guildId, String channelId) {
        if (channelId == null) {
            return Optional.empty();
        }
        return database.withConnection(connection -> Jdbc.queryOne(connection,
                "SELECT * FROM tickets WHERE guild_id = ? AND channel_id = ?", this::mapTicket, guildId, channelId));
    }

    @Override
    public Optional<Ticket> getTicketByChannelAnyGuild(String channelId) {
        if (channelId == null) {
            return Optional.empty();
        }
        return database.withConnection(connection -> Jdbc.queryOne(connection,
                "SELECT * FROM tickets WHERE channel_id = ?", this::mapTicket, channelId));
    }

    @Override
    public List<Ticket> getTickets(String guildId) {
        return database.withConnection(connection -> Jdbc.query(connection,
                "SELECT * FROM tickets WHERE guild_id = ? ORDER BY created_at", this::mapTicket, guildId));
    }

    @Override
    public List<Ticket> getOpenTickets(String guildId) {
        return database.withConnection(connection -> Jdbc.query(connection,
                "SELECT * FROM tickets WHERE guild_id = ? AND status != ? ORDER BY created_at",
                this::mapTicket, guildId, TicketStatus.CLOSED));
    }

    @Override
    public List<Ticket> getOpenTicketsByMember(String guildId, String memberId) {
        return database.withConnection(connection -> Jdbc.query(connection,
                "SELECT * FROM tickets WHERE guild_id = ? AND author_id = ? AND status != ? ORDER BY created_at",
                this::mapTicket, guildId, memberId, TicketStatus.CLOSED));
    }

    @Override
    public List<Ticket> getTicketsByMember(String guildId, String memberId) {
        return database.withConnection(connection -> Jdbc.query(connection,
                "SELECT * FROM tickets WHERE guild_id = ? AND author_id = ? ORDER BY created_at",
                this::mapTicket, guildId, memberId));
    }

    @Override
    public Map<String, List<Ticket>> getAllOpenTicketsByGuild() {
        List<Ticket> allOpen = database.withConnection(connection -> Jdbc.query(connection,
                "SELECT * FROM tickets WHERE status != ? ORDER BY guild_id, created_at",
                this::mapTicket, TicketStatus.CLOSED));
        Map<String, List<Ticket>> result = new ConcurrentHashMap<>();
        for (Ticket ticket : allOpen) {
            result.computeIfAbsent(ticket.getGuildId(), key -> new java.util.ArrayList<>()).add(ticket);
        }
        return result;
    }

    @Override
    public int pruneClosedTickets(Duration retention) {
        long threshold = Instant.now().minus(retention).toEpochMilli();
        return database.withConnection(connection -> Jdbc.update(connection,
                "DELETE FROM tickets WHERE status = ? AND closed_at > 0 AND closed_at < ?",
                TicketStatus.CLOSED, threshold));
    }

    private Ticket mapTicket(ResultSet resultSet) throws SQLException {
        String guildId = resultSet.getString("guild_id");
        String ticketId = resultSet.getString("ticket_id");
        Set<String> participants = new LinkedHashSet<>(loadParticipants(resultSet, guildId, ticketId));
        return new Ticket(ticketId, guildId, resultSet.getString("channel_id"),
                resultSet.getString("control_message_id"), resultSet.getString("author_id"),
                resultSet.getString("author_name"), TicketCategory.valueOf(resultSet.getString("category")),
                TicketPriority.valueOf(resultSet.getString("priority")),
                TicketStatus.valueOf(resultSet.getString("status")), resultSet.getString("subject"),
                resultSet.getString("description"), resultSet.getLong("created_at"),
                resultSet.getLong("updated_at"), resultSet.getLong("last_activity_at"),
                resultSet.getLong("closed_at"), resultSet.getString("claimed_by_id"),
                resultSet.getString("claimed_by_name"), resultSet.getString("closed_by_id"),
                resultSet.getString("closed_by_name"), resultSet.getString("close_reason"),
                resultSet.getString("transcript_file_name"), resultSet.getString("log_channel_id"),
                resultSet.getString("log_message_id"), Jdbc.getNullableInt(resultSet, "rating_score"),
                resultSet.getString("rating_comment"), Jdbc.getBoolean(resultSet, "auto_close_warning_sent"),
                participants);
    }

    private List<String> loadParticipants(ResultSet outer, String guildId, String ticketId) throws SQLException {
        Connection connection = outer.getStatement().getConnection();
        return Jdbc.query(connection,
                "SELECT member_id FROM ticket_participants WHERE guild_id = ? AND ticket_id = ?",
                rs -> rs.getString("member_id"), guildId, ticketId);
    }

    @Override
    public boolean claim(String guildId, String ticketId, String modId, String modName) {
        return mutateOpenTicket(guildId, ticketId, ticket -> ticket.claim(modId, modName));
    }

    @Override
    public boolean unclaim(String guildId, String ticketId) {
        return mutateOpenTicket(guildId, ticketId, Ticket::unclaim);
    }

    @Override
    public boolean setPriority(String guildId, String ticketId, TicketPriority priority) {
        return mutateOpenTicket(guildId, ticketId, ticket -> ticket.setPriority(priority));
    }

    @Override
    public boolean close(String guildId, String ticketId, String modId, String modName, String reason) {
        return mutateOpenTicket(guildId, ticketId, ticket -> ticket.close(modId, modName, reason));
    }

    @Override
    public boolean rate(String guildId, String ticketId, int score, String comment) {
        if (score < 1 || score > 5) {
            return false;
        }
        return mutateTicket(guildId, ticketId, ticket -> true, ticket -> ticket.rate(score, comment));
    }

    @Override
    public boolean addParticipant(String guildId, String ticketId, String memberId) {
        Optional<Ticket> ticketOpt = getTicket(guildId, ticketId);
        if (ticketOpt.isEmpty() || ticketOpt.get().getStatus().isClosed()) {
            return false;
        }
        Ticket ticket = ticketOpt.get();
        boolean added = ticket.addParticipant(memberId);
        if (added) {
            database.runInTransaction(connection -> {
                Jdbc.update(connection,
                        "INSERT OR IGNORE INTO ticket_participants (guild_id, ticket_id, member_id) VALUES (?, ?, ?)",
                        guildId, ticketId, memberId);
                updateTicketRow(connection, ticket);
            });
        }
        return added;
    }

    @Override
    public boolean removeParticipant(String guildId, String ticketId, String memberId) {
        Optional<Ticket> ticketOpt = getTicket(guildId, ticketId);
        if (ticketOpt.isEmpty() || ticketOpt.get().getStatus().isClosed()) {
            return false;
        }
        Ticket ticket = ticketOpt.get();
        boolean removed = ticket.removeParticipant(memberId);
        if (removed) {
            database.runInTransaction(connection -> {
                Jdbc.update(connection,
                        "DELETE FROM ticket_participants WHERE guild_id = ? AND ticket_id = ? AND member_id = ?",
                        guildId, ticketId, memberId);
                updateTicketRow(connection, ticket);
            });
        }
        return removed;
    }

    @Override
    public void attachChannel(String guildId, String ticketId, String channelId) {
        mutateTicket(guildId, ticketId, ticket -> true, ticket -> ticket.setChannelId(channelId));
    }

    @Override
    public void attachControlMessage(String guildId, String ticketId, String messageId) {
        mutateTicket(guildId, ticketId, ticket -> true, ticket -> ticket.setControlMessageId(messageId));
    }

    @Override
    public void attachLogMessage(String guildId, String ticketId, String logChannelId, String logMessageId) {
        mutateTicket(guildId, ticketId, ticket -> true, ticket -> ticket.setLogMessage(logChannelId, logMessageId));
    }

    @Override
    public void attachTranscript(String guildId, String ticketId, String transcriptFileName) {
        mutateTicket(guildId, ticketId, ticket -> true, ticket -> ticket.setTranscriptFileName(transcriptFileName));
    }

    @Override
    public void markAutoCloseWarned(String guildId, String ticketId) {
        mutateTicket(guildId, ticketId, ticket -> true, ticket -> ticket.setAutoCloseWarningSent(true));
    }

    @Override
    public void recordActivity(String guildId, String channelId) {
        getTicketByChannel(guildId, channelId).ifPresent(ticket -> {
            ticket.recordActivity();
            save(ticket);
        });
    }

    private boolean mutateOpenTicket(String guildId, String ticketId, java.util.function.Consumer<Ticket> mutation) {
        return mutateTicket(guildId, ticketId, ticket -> !ticket.getStatus().isClosed(), mutation);
    }

    private boolean mutateTicket(String guildId, String ticketId, java.util.function.Predicate<Ticket> guard,
                                  java.util.function.Consumer<Ticket> mutation) {
        Optional<Ticket> ticketOpt = getTicket(guildId, ticketId);
        if (ticketOpt.isEmpty() || !guard.test(ticketOpt.get())) {
            return false;
        }
        Ticket ticket = ticketOpt.get();
        mutation.accept(ticket);
        save(ticket);
        return true;
    }
}
