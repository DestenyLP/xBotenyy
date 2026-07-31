package de.destenylp.xBotenyy.discordbot.moderation;

import de.destenylp.xBotenyy.common.moderation.AccountLinkRepository;
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
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class DiscordModerationBridgeHandler implements ModerationBridgeHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordModerationBridgeHandler.class);

    private final JDA jda;
    private final String syncGuildId;
    private final DiscordModerationService moderationService;
    private final AccountLinkRepository accountLinkRepository;
    private final PendingLinkVerificationRepository pendingLinkVerificationRepository;

    public DiscordModerationBridgeHandler(JDA jda, String syncGuildId, DiscordModerationService moderationService,
                                           AccountLinkRepository accountLinkRepository,
                                           PendingLinkVerificationRepository pendingLinkVerificationRepository) {
        this.jda = jda;
        this.syncGuildId = syncGuildId;
        this.moderationService = moderationService;
        this.accountLinkRepository = accountLinkRepository;
        this.pendingLinkVerificationRepository = pendingLinkVerificationRepository;
    }

    @Override
    public BridgeActionResult applyAction(BridgeActionRequest request) {
        if (syncGuildId == null || syncGuildId.isBlank()) {
            return new BridgeActionResult(false, "Kein Sync-Server konfiguriert (moderation.sync.guild.id).");
        }
        Guild guild = jda.getGuildById(syncGuildId);
        if (guild == null) {
            return new BridgeActionResult(false, "Sync-Server nicht gefunden.");
        }
        if (request.targetUserId() == null || request.targetUserId().isBlank()) {
            return new BridgeActionResult(false, "Keine Discord-Nutzer-ID uebermittelt.");
        }

        java.util.concurrent.CompletableFuture<BridgeActionResult> future = new java.util.concurrent.CompletableFuture<>();
        moderationService.applySyncedAction(guild, request.targetUserId(), request.targetLogin(), request.action(),
                "Sync von Twitch: " + request.reason(), request.durationSeconds(),
                () -> future.complete(new BridgeActionResult(true, "OK")),
                failure -> {
                    LOGGER.warn("Synchronisierte Aktion {} fuer {} fehlgeschlagen: {}", request.action(),
                            request.targetUserId(), failure.getMessage());
                    future.complete(new BridgeActionResult(false, failure.getMessage()));
                });
        try {
            return future.get(15, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            return new BridgeActionResult(false, "Zeitueberschreitung bei der Ausfuehrung.");
        }
    }

    @Override
    public BridgeLinkConfirmResult confirmLink(BridgeLinkConfirmRequest request) {
        Optional<PendingLinkVerification> pendingOpt = pendingLinkVerificationRepository.consume(request.code());
        if (pendingOpt.isEmpty()) {
            return new BridgeLinkConfirmResult(false, null, null, null, null, "Code ungueltig oder abgelaufen.");
        }
        PendingLinkVerification pending = pendingOpt.get();
        if (pending.initiatedFrom() != ModerationPlatform.DISCORD) {
            return new BridgeLinkConfirmResult(false, null, null, null, null, "Code wurde nicht auf Discord erzeugt.");
        }
        if (pending.twitchLogin() != null && !pending.twitchLogin().isBlank()
                && !pending.twitchLogin().equalsIgnoreCase(request.twitchLogin())) {
            return new BridgeLinkConfirmResult(false, null, null, null, null, "Twitch-Account stimmt nicht mit der Anfrage ueberein.");
        }
        accountLinkRepository.save(pending.discordUserId(), request.twitchUserId(), request.twitchLogin());
        LOGGER.info("Account-Verknuepfung bestaetigt: Discord {} <-> Twitch {}", pending.discordUserId(), request.twitchLogin());
        return new BridgeLinkConfirmResult(true, pending.discordUserId(), pending.discordUsername(),
                request.twitchUserId(), request.twitchLogin(), "Verknuepfung erfolgreich.");
    }

    @Override
    public BridgeRoleSyncResult syncRoles(BridgeRoleSyncRequest request) {
        if (syncGuildId == null || syncGuildId.isBlank()) {
            return new BridgeRoleSyncResult(false, "Kein Sync-Server konfiguriert (moderation.sync.guild.id).");
        }
        Guild guild = jda.getGuildById(syncGuildId);
        if (guild == null) {
            return new BridgeRoleSyncResult(false, "Sync-Server nicht gefunden.");
        }
        Member member = guild.getMemberById(request.targetDiscordUserId());
        if (member == null) {
            return new BridgeRoleSyncResult(false, "Nutzer ist kein Mitglied des Sync-Servers.");
        }
        moderationService.syncTwitchRoles(guild, member, request.statuses());
        return new BridgeRoleSyncResult(true, "OK");
    }
}
