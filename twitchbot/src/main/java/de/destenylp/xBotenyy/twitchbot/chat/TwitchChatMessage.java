package de.destenylp.xBotenyy.twitchbot.chat;

public record TwitchChatMessage(
        String channelLogin,
        String messageId,
        String userId,
        String userLogin,
        String displayName,
        String content,
        boolean moderator,
        boolean broadcaster,
        boolean subscriber,
        boolean vip) {
    public boolean isPrivileged() {
        return moderator || broadcaster;
    }
}
