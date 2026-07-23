package de.destenylp.xBotenyy.discordbot.reactionroles;

import java.util.List;
import java.util.Optional;

public interface ReactionRoleRepository {
    ReactionRoleMessage createMessage(String guildId, String channelId, String messageId);
    ReactionRoleMessage getOrCreateMessage(String guildId, String channelId, String messageId);
    Optional<ReactionRoleMessage> getMessage(String guildId, String messageId);
    List<ReactionRoleMessage> getMessages(String guildId);
    boolean addEntry(String guildId, String messageId, ReactionRoleEntry entry);
    boolean removeEntry(String guildId, String messageId, String identifier);
}
