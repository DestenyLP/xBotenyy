package de.destenylp.xBotenyy.discordbot.moderation;

import java.util.List;

public record ModerationRoleSettings(
        String guildId,
        String warnRoleId,
        String muteRoleId,
        String banRoleId,
        List<String> moderatorRoleIds,
        List<String> adminRoleIds,
        String syncSubscriberRoleId,
        String syncVipRoleId,
        String syncModeratorRoleId,
        String syncBroadcasterRoleId) {
    public static ModerationRoleSettings empty(String guildId) {
        return new ModerationRoleSettings(guildId, null, null, null, List.of(), List.of(), null, null, null, null);
    }
}

