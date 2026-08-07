package de.destenylp.xBotenyy.twitchbot.chat;

public record TwitchFollowEvent(
        String channelLogin,
        String userId,
        String userLogin,
        String displayName) {
}

