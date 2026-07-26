package de.destenylp.xBotenyy.twitchbot.chat;

public record TwitchAutomodHeldMessage(
        String channelLogin,
        String messageId,
        String userId,
        String userLogin,
        String content,
        String category,
        String level) {
}
