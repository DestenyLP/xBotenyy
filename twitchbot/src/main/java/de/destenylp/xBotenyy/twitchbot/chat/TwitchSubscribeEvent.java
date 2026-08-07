package de.destenylp.xBotenyy.twitchbot.chat;

public record TwitchSubscribeEvent(
        String channelLogin,
        String userId,
        String userLogin,
        String displayName,
        String tier,
        boolean gift) {
}

