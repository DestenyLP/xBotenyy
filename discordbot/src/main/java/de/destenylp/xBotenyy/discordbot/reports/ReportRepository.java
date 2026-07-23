package de.destenylp.xBotenyy.discordbot.reports;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface ReportRepository {
    Optional<ReportSettings> getSettings(String guildId);
    void updateChannel(String guildId, String channelId);
    void updateNotifyRole(String guildId, String roleId);
    Report createReport(Report draft);
    void save(Report report);
    Optional<Report> getReport(String guildId, String id);
    List<Report> getReports(String guildId);
    List<Report> getReportsByMember(String guildId, String memberId);
    int pruneClosedReports(Duration retention);
}
