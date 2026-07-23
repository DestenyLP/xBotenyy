package de.destenylp.xBotenyy.discordbot.giveaways;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Giveaway {
    private String id;
    private final String guildId;
    private String channelId;
    private String messageId;
    private final String prize;
    private final String description;
    private final int winnerCount;
    private final String hostId;
    private final String hostName;
    private final String requiredRoleId;
    private GiveawayStatus status;
    private final long createdAt;
    private final long endAt;
    private long endedAt;
    private final Set<String> participantIds = new LinkedHashSet<>();
    private List<String> winnerIds = new ArrayList<>();

    public Giveaway(String guildId, String prize, String description, int winnerCount, String hostId,
                     String hostName, String requiredRoleId, long endAt) {
        this.guildId = guildId;
        this.prize = prize;
        this.description = description;
        this.winnerCount = winnerCount;
        this.hostId = hostId;
        this.hostName = hostName;
        this.requiredRoleId = requiredRoleId;
        this.endAt = endAt;
        this.status = GiveawayStatus.RUNNING;
        this.createdAt = Instant.now().toEpochMilli();
    }

    Giveaway(String id, String guildId, String channelId, String messageId, String prize, String description,
             int winnerCount, String hostId, String hostName, String requiredRoleId, GiveawayStatus status,
             long createdAt, long endAt, long endedAt, Set<String> participantIds, List<String> winnerIds) {
        this.id = id;
        this.guildId = guildId;
        this.channelId = channelId;
        this.messageId = messageId;
        this.prize = prize;
        this.description = description;
        this.winnerCount = winnerCount;
        this.hostId = hostId;
        this.hostName = hostName;
        this.requiredRoleId = requiredRoleId;
        this.status = status;
        this.createdAt = createdAt;
        this.endAt = endAt;
        this.endedAt = endedAt;
        this.participantIds.addAll(participantIds);
        this.winnerIds = new ArrayList<>(winnerIds);
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

    public String getMessageId() {
        return messageId;
    }

    public void setChannelAndMessage(String channelId, String messageId) {
        this.channelId = channelId;
        this.messageId = messageId;
    }

    public String getPrize() {
        return prize;
    }

    public String getDescription() {
        return description;
    }

    public int getWinnerCount() {
        return winnerCount;
    }

    public String getHostId() {
        return hostId;
    }

    public String getHostName() {
        return hostName;
    }

    public String getRequiredRoleId() {
        return requiredRoleId;
    }

    public GiveawayStatus getStatus() {
        return status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getEndAt() {
        return endAt;
    }

    public long getEndedAt() {
        return endedAt;
    }

    public Set<String> getParticipantIds() {
        return Collections.unmodifiableSet(participantIds);
    }

    public boolean isParticipant(String memberId) {
        return participantIds.contains(memberId);
    }

    public boolean addParticipant(String memberId) {
        return participantIds.add(memberId);
    }

    public boolean removeParticipant(String memberId) {
        return participantIds.remove(memberId);
    }

    public List<String> getWinnerIds() {
        return Collections.unmodifiableList(winnerIds);
    }

    public boolean isRunning() {
        return status == GiveawayStatus.RUNNING;
    }

    public boolean isDue() {
        return isRunning() && Instant.now().toEpochMilli() >= endAt;
    }

    public void end(List<String> winnerIds) {
        this.winnerIds = new ArrayList<>(winnerIds);
        this.status = GiveawayStatus.ENDED;
        this.endedAt = Instant.now().toEpochMilli();
    }

    public void reroll(List<String> winnerIds) {
        this.winnerIds = new ArrayList<>(winnerIds);
        this.endedAt = Instant.now().toEpochMilli();
    }

    public void cancel() {
        this.status = GiveawayStatus.CANCELLED;
        this.endedAt = Instant.now().toEpochMilli();
    }
}
