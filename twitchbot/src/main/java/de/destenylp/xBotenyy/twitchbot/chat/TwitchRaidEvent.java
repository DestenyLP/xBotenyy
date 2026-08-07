package de.destenylp.xBotenyy.twitchbot.chat;
public record TwitchRaidEvent(
        String channelLogin,
        String fromUserId,
        String fromUserLogin,
        String displayName,
        int viewers) {
}
