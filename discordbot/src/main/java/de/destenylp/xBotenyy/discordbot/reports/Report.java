package de.destenylp.xBotenyy.discordbot.reports;

import java.time.Instant;

public class Report {
    private String id;
    private final String guildId;
    private final String reporterId;
    private final String reporterName;
    private final ReportCategory category;
    private final String subject;
    private final String target;
    private final String description;
    private final String evidence;
    private ReportStatus status;
    private final long createdAt;
    private long updatedAt;
    private String reportChannelId;
    private String reportMessageId;
    private String assignedModId;
    private String assignedModName;
    private String resolutionNote;
    private String rejectionReason;

    public Report(String guildId, String reporterId, String reporterName, ReportCategory category,
                  String subject, String target, String description, String evidence) {
        this.guildId = guildId;
        this.reporterId = reporterId;
        this.reporterName = reporterName;
        this.category = category;
        this.subject = subject;
        this.target = target;
        this.description = description;
        this.evidence = evidence;
        this.status = ReportStatus.OPEN;
        this.createdAt = Instant.now().toEpochMilli();
        this.updatedAt = this.createdAt;
    }

    Report(String id, String guildId, String reporterId, String reporterName, ReportCategory category,
           String subject, String target, String description, String evidence, ReportStatus status,
           long createdAt, long updatedAt, String reportChannelId, String reportMessageId, String assignedModId,
           String assignedModName, String resolutionNote, String rejectionReason) {
        this.id = id;
        this.guildId = guildId;
        this.reporterId = reporterId;
        this.reporterName = reporterName;
        this.category = category;
        this.subject = subject;
        this.target = target;
        this.description = description;
        this.evidence = evidence;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.reportChannelId = reportChannelId;
        this.reportMessageId = reportMessageId;
        this.assignedModId = assignedModId;
        this.assignedModName = assignedModName;
        this.resolutionNote = resolutionNote;
        this.rejectionReason = rejectionReason;
    }

    void assignId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getGuildId() {
        return guildId;
    }

    public String getReporterId() {
        return reporterId;
    }

    public String getReporterName() {
        return reporterName;
    }

    public ReportCategory getCategory() {
        return category;
    }

    public String getSubject() {
        return subject;
    }

    public String getTarget() {
        return target;
    }

    public String getDescription() {
        return description;
    }

    public String getEvidence() {
        return evidence;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void touch() {
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public String getReportChannelId() {
        return reportChannelId;
    }

    public void setReportChannelId(String reportChannelId) {
        this.reportChannelId = reportChannelId;
    }

    public String getReportMessageId() {
        return reportMessageId;
    }

    public void setReportMessageId(String reportMessageId) {
        this.reportMessageId = reportMessageId;
    }

    public String getAssignedModId() {
        return assignedModId;
    }

    public void setAssignedModId(String assignedModId) {
        this.assignedModId = assignedModId;
    }

    public String getAssignedModName() {
        return assignedModName;
    }

    public void setAssignedModName(String assignedModName) {
        this.assignedModName = assignedModName;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
