package de.destenylp.xBotenyy.discordbot.reports;

import de.destenylp.xBotenyy.discordbot.core.PrunableGuildService;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class ReportService implements PrunableGuildService {
    private final ReportRepository manager;

    public ReportService(ReportRepository manager) {
        this.manager = manager;
    }

    @Override
    public String getServiceName() {
        return "Reports";
    }

    @Override
    public int pruneOldEntries(Duration retention) {
        return pruneClosedReports(retention);
    }

    public Optional<ReportSettings> getSettings(String guildId) {
        return manager.getSettings(guildId);
    }

    public void updateChannel(String guildId, String channelId) {
        manager.updateChannel(guildId, channelId);
    }

    public void updateNotifyRole(String guildId, String roleId) {
        manager.updateNotifyRole(guildId, roleId);
    }

    public Report createReport(String guildId, String reporterId, String reporterName, ReportCategory category,
                                String subject, String target, String description, String evidence) {
        Report draft = new Report(guildId, reporterId, reporterName, category, subject, target, description, evidence);
        return manager.createReport(draft);
    }

    public Optional<Report> getReport(String guildId, String id) {
        return manager.getReport(guildId, id);
    }

    public List<Report> getReports(String guildId) {
        return manager.getReports(guildId);
    }

    public List<Report> getReportsByMember(String guildId, String memberId) {
        return manager.getReportsByMember(guildId, memberId);
    }

    public void attachMessage(String guildId, String id, String channelId, String messageId) {
        getReport(guildId, id).ifPresent(report -> {
            report.setReportChannelId(channelId);
            report.setReportMessageId(messageId);
            manager.save(report);
        });
    }

    public boolean claim(String guildId, String id, String modId, String modName) {
        Optional<Report> reportOpt = getReport(guildId, id);
        if (reportOpt.isEmpty() || reportOpt.get().getStatus().isClosed()) {
            return false;
        }

        Report report = reportOpt.get();
        report.setStatus(ReportStatus.IN_PROGRESS);
        report.setAssignedModId(modId);
        report.setAssignedModName(modName);
        report.touch();
        manager.save(report);
        return true;
    }

    public boolean resolve(String guildId, String id, String modId, String modName, String note) {
        Optional<Report> reportOpt = getReport(guildId, id);
        if (reportOpt.isEmpty() || reportOpt.get().getStatus().isClosed()) {
            return false;
        }

        Report report = reportOpt.get();
        report.setStatus(ReportStatus.RESOLVED);
        report.setAssignedModId(modId);
        report.setAssignedModName(modName);
        report.setResolutionNote(note);
        report.touch();
        manager.save(report);
        return true;
    }

    public boolean reject(String guildId, String id, String modId, String modName, String reason) {
        Optional<Report> reportOpt = getReport(guildId, id);
        if (reportOpt.isEmpty() || reportOpt.get().getStatus().isClosed()) {
            return false;
        }

        Report report = reportOpt.get();
        report.setStatus(ReportStatus.REJECTED);
        report.setAssignedModId(modId);
        report.setAssignedModName(modName);
        report.setRejectionReason(reason);
        report.touch();
        manager.save(report);
        return true;
    }

    public static boolean isModerator(Member member, ReportSettings settings) {
        if (member.hasPermission(Permission.ADMINISTRATOR)
                || member.hasPermission(Permission.MODERATE_MEMBERS)
                || member.hasPermission(Permission.MESSAGE_MANAGE)) {
            return true;
        }

        if (settings != null && settings.getNotifyRoleId() != null) {
            return member.getRoles().stream().anyMatch(role -> role.getId().equals(settings.getNotifyRoleId()));
        }

        return false;
    }

    public int pruneClosedReports(Duration retention) {
        return manager.pruneClosedReports(retention);
    }
}
