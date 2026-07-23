package de.destenylp.xBotenyy.discordbot.reports;

import de.destenylp.xBotenyy.common.persistence.sql.AbstractSqlManager;
import de.destenylp.xBotenyy.common.persistence.sql.Database;
import de.destenylp.xBotenyy.common.persistence.sql.Jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class ReportManager extends AbstractSqlManager implements ReportRepository {
    public ReportManager(Database database) {
        super(database);
    }

    @Override
    public Optional<ReportSettings> getSettings(String guildId) {
        return database.withConnection(connection -> Jdbc.queryOne(connection,
                "SELECT * FROM report_guild_settings WHERE guild_id = ?", resultSet -> mapSettings(guildId, resultSet),
                guildId));
    }

    private ReportSettings mapSettings(String guildId, ResultSet resultSet) throws SQLException {
        ReportSettings settings = new ReportSettings(() -> getReports(guildId));
        settings.setChannelId(Jdbc.getString(resultSet, "channel_id"));
        settings.setNotifyRoleId(Jdbc.getString(resultSet, "notify_role_id"));
        return settings;
    }

    @Override
    public void updateChannel(String guildId, String channelId) {
        database.runInTransaction(connection -> {
            ensureSettingsRow(connection, "report_guild_settings", guildId);
            Jdbc.update(connection, "UPDATE report_guild_settings SET channel_id = ? WHERE guild_id = ?",
                    channelId, guildId);
        });
    }

    @Override
    public void updateNotifyRole(String guildId, String roleId) {
        database.runInTransaction(connection -> {
            ensureSettingsRow(connection, "report_guild_settings", guildId);
            Jdbc.update(connection, "UPDATE report_guild_settings SET notify_role_id = ? WHERE guild_id = ?",
                    roleId, guildId);
        });
    }

    @Override
    public Report createReport(Report draft) {
        return database.inTransaction(connection -> {
            ensureSettingsRow(connection, "report_guild_settings", draft.getGuildId());
            String id = generateUniqueShortId(connection, "reports", "report_id", "guild_id", draft.getGuildId(), true);
            draft.assignId(id);
            Jdbc.update(connection, """
                    INSERT INTO reports (guild_id, report_id, reporter_id, reporter_name, category, subject, target,
                        description, evidence, status, created_at, updated_at, report_channel_id, report_message_id,
                        assigned_mod_id, assigned_mod_name, resolution_note, rejection_reason)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    draft.getGuildId(), draft.getId(), draft.getReporterId(), draft.getReporterName(),
                    draft.getCategory(), draft.getSubject(), draft.getTarget(), draft.getDescription(),
                    draft.getEvidence(), draft.getStatus(), draft.getCreatedAt(), draft.getUpdatedAt(),
                    draft.getReportChannelId(), draft.getReportMessageId(), draft.getAssignedModId(),
                    draft.getAssignedModName(), draft.getResolutionNote(), draft.getRejectionReason());
            return draft;
        });
    }

    @Override
    public void save(Report report) {
        database.runInTransaction(connection -> Jdbc.update(connection, """
                UPDATE reports SET status = ?, updated_at = ?, report_channel_id = ?, report_message_id = ?,
                    assigned_mod_id = ?, assigned_mod_name = ?, resolution_note = ?, rejection_reason = ?
                WHERE guild_id = ? AND report_id = ?
                """,
                report.getStatus(), report.getUpdatedAt(), report.getReportChannelId(), report.getReportMessageId(),
                report.getAssignedModId(), report.getAssignedModName(), report.getResolutionNote(),
                report.getRejectionReason(), report.getGuildId(), report.getId()));
    }

    @Override
    public Optional<Report> getReport(String guildId, String id) {
        return database.withConnection(connection -> Jdbc.queryOne(connection,
                "SELECT * FROM reports WHERE guild_id = ? AND report_id = ? COLLATE NOCASE", this::mapReport,
                guildId, id));
    }

    @Override
    public List<Report> getReports(String guildId) {
        return database.withConnection(connection -> Jdbc.query(connection,
                "SELECT * FROM reports WHERE guild_id = ? ORDER BY created_at", this::mapReport, guildId));
    }

    @Override
    public List<Report> getReportsByMember(String guildId, String memberId) {
        return database.withConnection(connection -> Jdbc.query(connection,
                "SELECT * FROM reports WHERE guild_id = ? AND reporter_id = ? ORDER BY created_at",
                this::mapReport, guildId, memberId));
    }

    @Override
    public int pruneClosedReports(Duration retention) {
        long threshold = Instant.now().minus(retention).toEpochMilli();
        return database.withConnection(connection -> Jdbc.update(connection,
                "DELETE FROM reports WHERE status IN (?, ?) AND updated_at < ?",
                ReportStatus.RESOLVED, ReportStatus.REJECTED, threshold));
    }

    private Report mapReport(ResultSet resultSet) throws SQLException {
        return new Report(resultSet.getString("report_id"), resultSet.getString("guild_id"),
                resultSet.getString("reporter_id"), resultSet.getString("reporter_name"),
                ReportCategory.valueOf(resultSet.getString("category")), resultSet.getString("subject"),
                resultSet.getString("target"), resultSet.getString("description"), resultSet.getString("evidence"),
                ReportStatus.valueOf(resultSet.getString("status")), resultSet.getLong("created_at"),
                resultSet.getLong("updated_at"), resultSet.getString("report_channel_id"),
                resultSet.getString("report_message_id"), resultSet.getString("assigned_mod_id"),
                resultSet.getString("assigned_mod_name"), resultSet.getString("resolution_note"),
                resultSet.getString("rejection_reason"));
    }
}
