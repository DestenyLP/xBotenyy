package de.destenylp.xBotenyy.common.moderation;

public record PendingLinkVerification(
        String code,
        String discordUserId,
        String discordUsername,
        String twitchUserId,
        String twitchLogin,
        ModerationPlatform initiatedFrom,
        long expiresAt) {
}
