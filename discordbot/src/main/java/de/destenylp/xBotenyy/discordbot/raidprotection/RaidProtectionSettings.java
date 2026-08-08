package de.destenylp.xBotenyy.discordbot.raidprotection;

public record RaidProtectionSettings(String guildId, boolean enabled, String alertChannelId) {
    public static RaidProtectionSettings empty(String guildId) {
        return new RaidProtectionSettings(guildId, false, null);
    }
}
