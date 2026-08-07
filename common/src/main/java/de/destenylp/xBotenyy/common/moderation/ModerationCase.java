package de.destenylp.xBotenyy.common.moderation;

public record ModerationCase(
        long id,
        ModerationPlatform platform,
        String scopeId,
        String targetId,
        String targetName,
        String moderatorId,
        String moderatorName,
        ModerationAction action,
        String reason,
        long durationSeconds,
        long createdAt,
        boolean active,
        boolean synced) {
}

