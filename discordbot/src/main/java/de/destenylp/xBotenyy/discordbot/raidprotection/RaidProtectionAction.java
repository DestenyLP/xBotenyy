package de.destenylp.xBotenyy.discordbot.raidprotection;

public enum RaidProtectionAction {
    KICK,
    BAN;

    public static RaidProtectionAction fromString(String value, RaidProtectionAction fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return RaidProtectionAction.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
