package de.destenylp.xBotenyy.twitchbot.chat;

public record TwitchAutomodUpdateMessage(
        String channelLogin,
        String messageId,
        String userId,
        String userLogin,
        String status,
        String moderatorLogin) {
}

