package de.destenylp.xBotenyy.discordbot.reactionroles;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ReactionRoleMessage {
    private final String guildId;
    private final String channelId;
    private final String messageId;
    private final CopyOnWriteArrayList<ReactionRoleEntry> entries = new CopyOnWriteArrayList<>();

    public ReactionRoleMessage(String guildId, String channelId, String messageId) {
        this.guildId = guildId;
        this.channelId = channelId;
        this.messageId = messageId;
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

    public List<ReactionRoleEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    void addEntry(ReactionRoleEntry entry) {
        entries.add(entry);
    }

    boolean removeEntry(String identifier) {
        return entries.removeIf(entry ->
                entry.getRoleId().equals(identifier) || (entry.getEmoji() != null && entry.getEmoji().equals(identifier)));
    }
}
