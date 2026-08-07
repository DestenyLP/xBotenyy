package de.destenylp.xBotenyy.twitchbot.alerts;

public record TwitchAlertSettings(
        boolean followChatEnabled, String followChatMessage, boolean followDiscordEnabled,
        boolean subscribeChatEnabled, String subscribeChatMessage, boolean subscribeDiscordEnabled,
        boolean raidChatEnabled, String raidChatMessage, boolean raidDiscordEnabled) {
    public boolean anyFollowAlertEnabled() {
        return followChatEnabled || followDiscordEnabled;
    }

    public boolean anySubscribeAlertEnabled() {
        return subscribeChatEnabled || subscribeDiscordEnabled;
    }

    public boolean anyRaidAlertEnabled() {
        return raidChatEnabled || raidDiscordEnabled;
    }
}

