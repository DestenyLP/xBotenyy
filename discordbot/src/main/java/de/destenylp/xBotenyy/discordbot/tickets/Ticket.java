package de.destenylp.xBotenyy.discordbot.tickets;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class Ticket {
    private String id;
    private final String guildId;
    private String channelId;
    private String controlMessageId;
    private final String authorId;
    private final String authorName;
    private final TicketCategory category;
    private TicketPriority priority;
    private TicketStatus status;
    private final String subject;
    private final String description;
    private final long createdAt;
    private long updatedAt;
    private long lastActivityAt;
    private long closedAt;
    private String claimedById;
    private String claimedByName;
    private String closedById;
    private String closedByName;
    private String closeReason;
    private String transcriptFileName;
    private String logChannelId;
    private String logMessageId;
    private Integer ratingScore;
    private String ratingComment;
    private boolean autoCloseWarningSent;
    private final Set<String> participantIds = new LinkedHashSet<>();

    public Ticket(String guildId, String authorId, String authorName, TicketCategory category,
                  String subject, String description) {
        this.guildId = guildId;
        this.authorId = authorId;
        this.authorName = authorName;
        this.category = category;
        this.priority = category.getDefaultPriority();
        this.subject = subject;
        this.description = description;
        this.status = TicketStatus.OPEN;
        this.createdAt = Instant.now().toEpochMilli();
        this.updatedAt = this.createdAt;
        this.lastActivityAt = this.createdAt;
    }

    Ticket(String id, String guildId, String channelId, String controlMessageId, String authorId,
           String authorName, TicketCategory category, TicketPriority priority, TicketStatus status,
           String subject, String description, long createdAt, long updatedAt, long lastActivityAt,
           long closedAt, String claimedById, String claimedByName, String closedById, String closedByName,
           String closeReason, String transcriptFileName, String logChannelId, String logMessageId,
           Integer ratingScore, String ratingComment, boolean autoCloseWarningSent, Set<String> participantIds) {
        this.id = id;
        this.guildId = guildId;
        this.channelId = channelId;
        this.controlMessageId = controlMessageId;
        this.authorId = authorId;
        this.authorName = authorName;
        this.category = category;
        this.priority = priority;
        this.status = status;
        this.subject = subject;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastActivityAt = lastActivityAt;
        this.closedAt = closedAt;
        this.claimedById = claimedById;
        this.claimedByName = claimedByName;
        this.closedById = closedById;
        this.closedByName = closedByName;
        this.closeReason = closeReason;
        this.transcriptFileName = transcriptFileName;
        this.logChannelId = logChannelId;
        this.logMessageId = logMessageId;
        this.ratingScore = ratingScore;
        this.ratingComment = ratingComment;
        this.autoCloseWarningSent = autoCloseWarningSent;
        this.participantIds.addAll(participantIds);
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

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getControlMessageId() {
        return controlMessageId;
    }

    public void setControlMessageId(String controlMessageId) {
        this.controlMessageId = controlMessageId;
    }

    public String getAuthorId() {
        return authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public TicketCategory getCategory() {
        return category;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public String getSubject() {
        return subject;
    }

    public String getDescription() {
        return description;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public long getLastActivityAt() {
        return lastActivityAt;
    }

    public void recordActivity() {
        this.lastActivityAt = Instant.now().toEpochMilli();
        this.autoCloseWarningSent = false;
    }

    public boolean isAutoCloseWarningSent() {
        return autoCloseWarningSent;
    }

    public void setAutoCloseWarningSent(boolean autoCloseWarningSent) {
        this.autoCloseWarningSent = autoCloseWarningSent;
    }

    public void touch() {
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public long getClosedAt() {
        return closedAt;
    }

    public String getClaimedById() {
        return claimedById;
    }

    public String getClaimedByName() {
        return claimedByName;
    }

    public void claim(String modId, String modName) {
        this.claimedById = modId;
        this.claimedByName = modName;
        this.status = TicketStatus.CLAIMED;
        touch();
    }

    public void unclaim() {
        this.claimedById = null;
        this.claimedByName = null;
        this.status = TicketStatus.OPEN;
        touch();
    }

    public String getClosedById() {
        return closedById;
    }

    public String getClosedByName() {
        return closedByName;
    }

    public String getCloseReason() {
        return closeReason;
    }

    public void close(String modId, String modName, String reason) {
        this.status = TicketStatus.CLOSED;
        this.closedById = modId;
        this.closedByName = modName;
        this.closeReason = reason;
        this.closedAt = Instant.now().toEpochMilli();
        touch();
    }

    public String getTranscriptFileName() {
        return transcriptFileName;
    }

    public void setTranscriptFileName(String transcriptFileName) {
        this.transcriptFileName = transcriptFileName;
    }

    public String getLogChannelId() {
        return logChannelId;
    }

    public String getLogMessageId() {
        return logMessageId;
    }

    public void setLogMessage(String logChannelId, String logMessageId) {
        this.logChannelId = logChannelId;
        this.logMessageId = logMessageId;
    }

    public Integer getRatingScore() {
        return ratingScore;
    }

    public String getRatingComment() {
        return ratingComment;
    }

    public void rate(int score, String comment) {
        this.ratingScore = score;
        this.ratingComment = comment;
        touch();
    }

    public Set<String> getParticipantIds() {
        return Collections.unmodifiableSet(participantIds);
    }

    public boolean addParticipant(String memberId) {
        boolean added = participantIds.add(memberId);
        if (added) {
            touch();
        }
        return added;
    }

    public boolean removeParticipant(String memberId) {
        boolean removed = participantIds.remove(memberId);
        if (removed) {
            touch();
        }
        return removed;
    }
}
