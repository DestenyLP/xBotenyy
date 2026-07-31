package de.destenylp.xBotenyy.discordbot.moderation;

import de.destenylp.xBotenyy.common.moderation.ModerationAction;
import de.destenylp.xBotenyy.common.moderation.ModerationCaseRepository;
import de.destenylp.xBotenyy.common.moderation.ModerationPlatform;
import de.destenylp.xBotenyy.common.moderation.TwitchRoleSyncStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

public class DiscordModerationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordModerationService.class);
    private static final String SYNC_MODERATOR_ID = "TWITCH_SYNC";
    private static final String SYNC_MODERATOR_NAME = "Twitch-Sync";

    private final ModerationCaseRepository caseRepository;
    private final ModerationRoleSettingsRepository roleSettingsRepository;

    public DiscordModerationService(ModerationCaseRepository caseRepository,
                                     ModerationRoleSettingsRepository roleSettingsRepository) {
        this.caseRepository = caseRepository;
        this.roleSettingsRepository = roleSettingsRepository;
    }

    public void warn(Guild guild, Member target, Member moderator, String reason,
                      Runnable onSuccess, Consumer<Throwable> onFailure) {
        applyRole(guild, target, roleSettingsRepository.getOrEmpty(guild.getId()).warnRoleId(), true, reason);
        recordCase(guild, target.getUser(), moderator.getUser(), ModerationAction.WARN, reason, 0);
        onSuccess.run();
    }

    public void timeout(Guild guild, Member target, Member moderator, String reason, Duration duration,
                         Runnable onSuccess, Consumer<Throwable> onFailure) {
        target.timeoutFor(duration).reason(reason).queue(unused -> {
            applyRole(guild, target, roleSettingsRepository.getOrEmpty(guild.getId()).muteRoleId(), true, reason);
            recordCase(guild, target.getUser(), moderator.getUser(), ModerationAction.TIMEOUT, reason, duration.getSeconds());
            onSuccess.run();
        }, onFailure::accept);
    }

    public void untimeout(Guild guild, Member target, Member moderator, String reason,
                           Runnable onSuccess, Consumer<Throwable> onFailure) {
        target.removeTimeout().reason(reason).queue(unused -> {
            applyRole(guild, target, roleSettingsRepository.getOrEmpty(guild.getId()).muteRoleId(), false, reason);
            caseRepository.deactivate(ModerationPlatform.DISCORD, guild.getId(), target.getId(), ModerationAction.TIMEOUT);
            recordCase(guild, target.getUser(), moderator.getUser(), ModerationAction.UNTIMEOUT, reason, 0);
            onSuccess.run();
        }, onFailure::accept);
    }

    public void kick(Guild guild, Member target, Member moderator, String reason,
                      Runnable onSuccess, Consumer<Throwable> onFailure) {
        User targetUser = target.getUser();
        guild.kick(target).reason(reason).queue(unused -> {
            recordCase(guild, targetUser, moderator.getUser(), ModerationAction.KICK, reason, 0);
            onSuccess.run();
        }, onFailure::accept);
    }

    public void ban(Guild guild, UserSnowflake target, String targetId, String targetName, Member moderator,
                     String reason, Runnable onSuccess, Consumer<Throwable> onFailure) {
        String banRoleId = roleSettingsRepository.getOrEmpty(guild.getId()).banRoleId();
        if (banRoleId != null && !banRoleId.isBlank()) {
            Role role = guild.getRoleById(banRoleId);
            Member member = guild.getMemberById(targetId);
            if (role != null && member != null) {
                guild.addRoleToMember(member, role).reason(reason).queue(unused -> {
                }, failure -> LOGGER.warn("Konnte Ban-Rolle in Guild {} nicht setzen: {}", guild.getId(), failure.getMessage()));
            }
        }
        guild.ban(target, 0, java.util.concurrent.TimeUnit.SECONDS).reason(reason).queue(unused -> {
            recordCase(guild, targetId, targetName, moderator.getUser(), ModerationAction.BAN, reason, 0);
            onSuccess.run();
        }, onFailure::accept);
    }

    public void unban(Guild guild, UserSnowflake target, String targetId, String targetName, Member moderator,
                       String reason, Runnable onSuccess, Consumer<Throwable> onFailure) {
        guild.unban(target).reason(reason).queue(unused -> {
            String banRoleId = roleSettingsRepository.getOrEmpty(guild.getId()).banRoleId();
            Member member = guild.getMemberById(targetId);
            if (banRoleId != null && !banRoleId.isBlank() && member != null) {
                Role role = guild.getRoleById(banRoleId);
                if (role != null) {
                    guild.removeRoleFromMember(member, role).reason(reason).queue(unused2 -> {
                    }, failure -> LOGGER.warn("Konnte Ban-Rolle in Guild {} nicht entfernen: {}", guild.getId(), failure.getMessage()));
                }
            }
            caseRepository.deactivate(ModerationPlatform.DISCORD, guild.getId(), targetId, ModerationAction.BAN);
            recordCase(guild, targetId, targetName, moderator.getUser(), ModerationAction.UNBAN, reason, 0);
            onSuccess.run();
        }, onFailure::accept);
    }

    public void applySyncedAction(Guild guild, String targetId, String targetName, ModerationAction action,
                                   String reason, long durationSeconds, Runnable onSuccess, Consumer<Throwable> onFailure) {
        UserSnowflake target = UserSnowflake.fromId(targetId);
        Member member = guild.getMemberById(targetId);
        switch (action) {
            case WARN -> {
                if (member != null) {
                    applyRole(guild, member, roleSettingsRepository.getOrEmpty(guild.getId()).warnRoleId(), true, reason);
                }
                recordSyncedCase(guild, targetId, targetName, action, reason, 0);
                onSuccess.run();
            }
            case TIMEOUT -> {
                if (member == null) {
                    onFailure.accept(new IllegalStateException("Zielnutzer ist kein Mitglied dieses Servers."));
                    return;
                }
                Duration duration = durationSeconds > 0 ? Duration.ofSeconds(durationSeconds) : Duration.ofMinutes(10);
                member.timeoutFor(duration).reason(reason).queue(unused -> {
                    applyRole(guild, member, roleSettingsRepository.getOrEmpty(guild.getId()).muteRoleId(), true, reason);
                    recordSyncedCase(guild, targetId, targetName, action, reason, duration.getSeconds());
                    onSuccess.run();
                }, onFailure::accept);
            }
            case UNTIMEOUT -> {
                if (member == null) {
                    onFailure.accept(new IllegalStateException("Zielnutzer ist kein Mitglied dieses Servers."));
                    return;
                }
                member.removeTimeout().reason(reason).queue(unused -> {
                    applyRole(guild, member, roleSettingsRepository.getOrEmpty(guild.getId()).muteRoleId(), false, reason);
                    caseRepository.deactivate(ModerationPlatform.DISCORD, guild.getId(), targetId, ModerationAction.TIMEOUT);
                    recordSyncedCase(guild, targetId, targetName, action, reason, 0);
                    onSuccess.run();
                }, onFailure::accept);
            }
            case KICK -> {
                if (member == null) {
                    onFailure.accept(new IllegalStateException("Zielnutzer ist kein Mitglied dieses Servers."));
                    return;
                }
                guild.kick(member).reason(reason).queue(unused -> {
                    recordSyncedCase(guild, targetId, targetName, action, reason, 0);
                    onSuccess.run();
                }, onFailure::accept);
            }
            case BAN -> guild.ban(target, 0, java.util.concurrent.TimeUnit.SECONDS).reason(reason).queue(unused -> {
                recordSyncedCase(guild, targetId, targetName, action, reason, 0);
                onSuccess.run();
            }, onFailure::accept);
            case UNBAN -> guild.unban(target).reason(reason).queue(unused -> {
                caseRepository.deactivate(ModerationPlatform.DISCORD, guild.getId(), targetId, ModerationAction.BAN);
                recordSyncedCase(guild, targetId, targetName, action, reason, 0);
                onSuccess.run();
            }, onFailure::accept);
            default -> onFailure.accept(new IllegalArgumentException("Unbekannte Aktion: " + action));
        }
    }

    public void syncTwitchRoles(Guild guild, Member member, List<TwitchRoleSyncStatus> activeStatuses) {
        ModerationRoleSettings settings = roleSettingsRepository.getOrEmpty(guild.getId());
        applySyncRole(guild, member, settings.syncSubscriberRoleId(), activeStatuses.contains(TwitchRoleSyncStatus.SUBSCRIBER));
        applySyncRole(guild, member, settings.syncVipRoleId(), activeStatuses.contains(TwitchRoleSyncStatus.VIP));
        applySyncRole(guild, member, settings.syncModeratorRoleId(), activeStatuses.contains(TwitchRoleSyncStatus.MODERATOR));
        applySyncRole(guild, member, settings.syncBroadcasterRoleId(), activeStatuses.contains(TwitchRoleSyncStatus.BROADCASTER));
    }

    private void applySyncRole(Guild guild, Member member, String roleId, boolean shouldHave) {
        if (roleId == null || roleId.isBlank()) {
            return;
        }
        Role role = guild.getRoleById(roleId);
        if (role == null) {
            return;
        }
        boolean hasRole = member.getRoles().contains(role);
        if (shouldHave && !hasRole) {
            guild.addRoleToMember(member, role).reason("Twitch-Rollen-Sync").queue(unused -> {
            }, failure -> LOGGER.warn("Konnte Sync-Rolle {} in Guild {} nicht setzen: {}", roleId, guild.getId(), failure.getMessage()));
        } else if (!shouldHave && hasRole) {
            guild.removeRoleFromMember(member, role).reason("Twitch-Rollen-Sync").queue(unused -> {
            }, failure -> LOGGER.warn("Konnte Sync-Rolle {} in Guild {} nicht entfernen: {}", roleId, guild.getId(), failure.getMessage()));
        }
    }

    private void applyRole(Guild guild, Member target, String roleId, boolean add, String reason) {
        if (roleId == null || roleId.isBlank()) {
            return;
        }
        Role role = guild.getRoleById(roleId);
        if (role == null) {
            return;
        }
        if (add) {
            guild.addRoleToMember(target, role).reason(reason).queue(unused -> {
            }, failure -> LOGGER.warn("Konnte Rolle {} in Guild {} nicht setzen: {}", roleId, guild.getId(), failure.getMessage()));
        } else {
            guild.removeRoleFromMember(target, role).reason(reason).queue(unused -> {
            }, failure -> LOGGER.warn("Konnte Rolle {} in Guild {} nicht entfernen: {}", roleId, guild.getId(), failure.getMessage()));
        }
    }

    private void recordCase(Guild guild, User target, User moderator, ModerationAction action, String reason, long durationSeconds) {
        recordCase(guild, target.getId(), target.getName(), moderator, action, reason, durationSeconds);
    }

    private void recordCase(Guild guild, String targetId, String targetName, User moderator, ModerationAction action,
                             String reason, long durationSeconds) {
        caseRepository.insert(ModerationPlatform.DISCORD, guild.getId(), targetId, targetName, moderator.getId(),
                moderator.getName(), action, reason, durationSeconds, false);
    }

    private void recordSyncedCase(Guild guild, String targetId, String targetName, ModerationAction action,
                                   String reason, long durationSeconds) {
        caseRepository.insert(ModerationPlatform.DISCORD, guild.getId(), targetId, targetName, SYNC_MODERATOR_ID,
                SYNC_MODERATOR_NAME, action, reason, durationSeconds, true);
    }
}
