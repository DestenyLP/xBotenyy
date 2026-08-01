package de.destenylp.xBotenyy.twitchbot.moderation;

import de.destenylp.xBotenyy.common.moderation.AccountLinkRepository;
import de.destenylp.xBotenyy.common.moderation.ModerationAction;
import de.destenylp.xBotenyy.common.moderation.ModerationCaseRepository;
import de.destenylp.xBotenyy.common.moderation.ModerationPlatform;
import de.destenylp.xBotenyy.common.moderation.PendingLinkVerification;
import de.destenylp.xBotenyy.common.moderation.PendingLinkVerificationRepository;
import de.destenylp.xBotenyy.common.moderation.bridge.BridgeActionRequest;
import de.destenylp.xBotenyy.common.moderation.bridge.BridgeActionResult;
import de.destenylp.xBotenyy.common.moderation.bridge.BridgeLinkConfirmRequest;
import de.destenylp.xBotenyy.common.moderation.bridge.BridgeLinkConfirmResult;
import de.destenylp.xBotenyy.common.moderation.bridge.BridgeRoleSyncRequest;
import de.destenylp.xBotenyy.common.moderation.bridge.BridgeRoleSyncResult;
import de.destenylp.xBotenyy.common.moderation.bridge.ModerationBridgeHandler;
import de.destenylp.xBotenyy.twitchbot.automod.TwitchModerationApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class TwitchModerationBridgeHandler implements ModerationBridgeHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TwitchModerationBridgeHandler.class);
    private static final String SYNC_MODERATOR_ID = "DISCORD_SYNC";
    private static final String SYNC_MODERATOR_NAME = "Discord-Sync";

    private final String syncChannel;
    private final String moderatorUserId;
    private final TwitchModerationApiClient moderationApiClient;
    private final ModerationCaseRepository caseRepository;
    private final AccountLinkRepository accountLinkRepository;
    private final PendingLinkVerificationRepository pendingLinkVerificationRepository;

    public TwitchModerationBridgeHandler(String syncChannel, String moderatorUserId,
                                          TwitchModerationApiClient moderationApiClient,
                                          ModerationCaseRepository caseRepository,
                                          AccountLinkRepository accountLinkRepository,
                                          PendingLinkVerificationRepository pendingLinkVerificationRepository) {
        this.syncChannel = syncChannel;
        this.moderatorUserId = moderatorUserId;
        this.moderationApiClient = moderationApiClient;
        this.caseRepository = caseRepository;
        this.accountLinkRepository = accountLinkRepository;
        this.pendingLinkVerificationRepository = pendingLinkVerificationRepository;
    }

    @Override
    public BridgeActionResult applyAction(BridgeActionRequest request) {
        if (syncChannel == null || syncChannel.isBlank()) {
            return new BridgeActionResult(false, "Kein Sync-Kanal konfiguriert (moderation.sync.channel).");
        }
        if (request.targetUserId() == null || request.targetUserId().isBlank()) {
            return new BridgeActionResult(false, "Keine Twitch-Nutzer-ID uebermittelt.");
        }
        Optional<String> broadcasterId = moderationApiClient.resolveUserId(syncChannel);
        if (broadcasterId.isEmpty()) {
            return new BridgeActionResult(false, "Sync-Kanal konnte nicht aufgeloest werden.");
        }

        String reason = "Sync von Discord: " + request.reason();
        boolean success = switch (request.action()) {
            case TIMEOUT -> moderationApiClient.banUser(broadcasterId.get(), moderatorUserId, request.targetUserId(),
                    reason, request.durationSeconds() > 0 ? request.durationSeconds() : 600);
            case BAN -> moderationApiClient.banUser(broadcasterId.get(), moderatorUserId, request.targetUserId(), reason, 0);
            case UNBAN, UNTIMEOUT -> moderationApiClient.unbanUser(broadcasterId.get(), moderatorUserId, request.targetUserId());
            case WARN -> true;
            default -> false;
        };

        if (!success) {
            return new BridgeActionResult(false, "Aktion konnte auf Twitch nicht ausgefuehrt werden.");
        }

        ModerationAction resolvedAction = request.action() == ModerationAction.UNTIMEOUT
                ? ModerationAction.UNBAN : request.action();
        if (resolvedAction == ModerationAction.BAN || resolvedAction == ModerationAction.TIMEOUT) {
            caseRepository.insert(ModerationPlatform.TWITCH, syncChannel, request.targetUserId(), request.targetLogin(),
                    SYNC_MODERATOR_ID, SYNC_MODERATOR_NAME, resolvedAction, reason, request.durationSeconds(), true);
        } else if (resolvedAction == ModerationAction.UNBAN) {
            caseRepository.deactivate(ModerationPlatform.TWITCH, syncChannel, request.targetUserId(), ModerationAction.BAN);
            caseRepository.insert(ModerationPlatform.TWITCH, syncChannel, request.targetUserId(), request.targetLogin(),
                    SYNC_MODERATOR_ID, SYNC_MODERATOR_NAME, ModerationAction.UNBAN, reason, 0, true);
        }
        return new BridgeActionResult(true, "OK");
    }

    @Override
    public BridgeLinkConfirmResult confirmLink(BridgeLinkConfirmRequest request) {
        Optional<PendingLinkVerification> pendingOpt = pendingLinkVerificationRepository.consume(request.code());
        if (pendingOpt.isEmpty()) {
            return new BridgeLinkConfirmResult(false, null, null, null, null, "Code ungueltig oder abgelaufen.");
        }
        PendingLinkVerification pending = pendingOpt.get();
        if (pending.initiatedFrom() != ModerationPlatform.TWITCH) {
            return new BridgeLinkConfirmResult(false, null, null, null, null, "Code wurde nicht auf Twitch erzeugt.");
        }
        accountLinkRepository.save(request.discordUserId(), pending.twitchUserId(), pending.twitchLogin());
        LOGGER.info("Account link confirmed: Twitch {} <-> Discord {}", pending.twitchLogin(), request.discordUserId());
        return new BridgeLinkConfirmResult(true, request.discordUserId(), request.discordUsername(),
                pending.twitchUserId(), pending.twitchLogin(), "Verknuepfung erfolgreich.");
    }

    @Override
    public BridgeRoleSyncResult syncRoles(BridgeRoleSyncRequest request) {
        return new BridgeRoleSyncResult(false, "Rollen-Sync wird nur in Richtung Discord unterstuetzt.");
    }
}
