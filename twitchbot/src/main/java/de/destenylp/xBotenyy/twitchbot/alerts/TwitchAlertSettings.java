package de.destenylp.xBotenyy.twitchbot.alerts;
/**
 * Configuration for the follow/subscribe/raid alerts. Chat and Discord delivery can be toggled
 * independently for each event type, and all chat message templates are customizable via
 * placeholders ({@code {user}}, {@code {channel}}, {@code {tier}} for subs, {@code {viewers}} for raids).
 */
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
