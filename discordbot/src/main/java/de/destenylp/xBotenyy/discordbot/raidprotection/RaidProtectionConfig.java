package de.destenylp.xBotenyy.discordbot.raidprotection;

import java.util.Set;

public record RaidProtectionConfig(
        int joinWindowSeconds,
        int joinThreshold,
        long raidModeDurationSeconds,
        RaidProtectionAction raidModeAction,
        String raidModeReason,
        int accountMinAgeMinutes,
        RaidProtectionAction accountMinAgeAction,
        String accountMinAgeReason,
        boolean botAutoKickEnabled,
        Set<String> botWhitelistIds,
        String botKickReason,
        boolean alertEnabled) {
}
