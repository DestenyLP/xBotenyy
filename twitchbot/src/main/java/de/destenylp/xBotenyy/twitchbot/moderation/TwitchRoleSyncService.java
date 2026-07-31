package de.destenylp.xBotenyy.twitchbot.moderation;

import de.destenylp.xBotenyy.common.moderation.AccountLink;
import de.destenylp.xBotenyy.common.moderation.AccountLinkRepository;
import de.destenylp.xBotenyy.common.moderation.TwitchRoleSyncStatus;
import de.destenylp.xBotenyy.common.moderation.bridge.BridgeRoleSyncRequest;
import de.destenylp.xBotenyy.common.moderation.bridge.BridgeSettings;
import de.destenylp.xBotenyy.common.moderation.bridge.ModerationBridgeClient;
import de.destenylp.xBotenyy.twitchbot.automod.TwitchModerationApiClient;
import de.destenylp.xBotenyy.twitchbot.chat.TwitchChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class TwitchRoleSyncService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TwitchRoleSyncService.class);

    private final AccountLinkRepository accountLinkRepository;
    private final ModerationBridgeClient bridgeClient;
    private final Supplier<BridgeSettings> bridgeSettingsSupplier;
    private final Map<String, List<TwitchRoleSyncStatus>> lastSyncedStatuses = new ConcurrentHashMap<>();

    public TwitchRoleSyncService(AccountLinkRepository accountLinkRepository, ModerationBridgeClient bridgeClient,
                                  Supplier<BridgeSettings> bridgeSettingsSupplier) {
        this.accountLinkRepository = accountLinkRepository;
        this.bridgeClient = bridgeClient;
        this.bridgeSettingsSupplier = bridgeSettingsSupplier;
    }

    public void reconcile(String channelLogin, TwitchModerationApiClient moderationApiClient) {
        BridgeSettings settings = bridgeSettingsSupplier.get();
        if (!settings.isPeerConfigured() || !moderationApiClient.hasBroadcasterAccessToken()) {
            return;
        }
        Optional<String> broadcasterIdOpt = moderationApiClient.resolveUserId(channelLogin);
        if (broadcasterIdOpt.isEmpty()) {
            return;
        }
        String broadcasterId = broadcasterIdOpt.get();

        Set<String> subscribers = moderationApiClient.getSubscriberUserIds(broadcasterId);
        Set<String> vips = moderationApiClient.getVipUserIds(broadcasterId);
        Set<String> moderators = moderationApiClient.getModeratorUserIds(broadcasterId);

        for (AccountLink link : accountLinkRepository.findAll()) {
            List<TwitchRoleSyncStatus> statuses = new ArrayList<>();
            if (link.twitchUserId().equals(broadcasterId)) {
                statuses.add(TwitchRoleSyncStatus.BROADCASTER);
            }
            if (moderators.contains(link.twitchUserId())) {
                statuses.add(TwitchRoleSyncStatus.MODERATOR);
            }
            if (vips.contains(link.twitchUserId())) {
                statuses.add(TwitchRoleSyncStatus.VIP);
            }
            if (subscribers.contains(link.twitchUserId())) {
                statuses.add(TwitchRoleSyncStatus.SUBSCRIBER);
            }
            syncIfChanged(link, statuses);
        }
    }

    public void handleMessage(TwitchChatMessage message) {
        BridgeSettings settings = bridgeSettingsSupplier.get();
        if (!settings.isPeerConfigured()) {
            return;
        }
        Optional<AccountLink> linkOpt = accountLinkRepository.findByTwitchUserId(message.userId());
        if (linkOpt.isEmpty()) {
            return;
        }

        List<TwitchRoleSyncStatus> statuses = new ArrayList<>();
        if (message.subscriber()) {
            statuses.add(TwitchRoleSyncStatus.SUBSCRIBER);
        }
        if (message.vip()) {
            statuses.add(TwitchRoleSyncStatus.VIP);
        }
        if (message.moderator()) {
            statuses.add(TwitchRoleSyncStatus.MODERATOR);
        }
        if (message.broadcaster()) {
            statuses.add(TwitchRoleSyncStatus.BROADCASTER);
        }

        syncIfChanged(linkOpt.get(), statuses);
    }

    private void syncIfChanged(AccountLink link, List<TwitchRoleSyncStatus> statuses) {
        List<TwitchRoleSyncStatus> previous = lastSyncedStatuses.get(link.twitchUserId());
        if (previous != null && previous.equals(statuses)) {
            return;
        }

        BridgeSettings settings = bridgeSettingsSupplier.get();
        bridgeClient.sendRoleSync(settings.peerUrl(), settings.token(),
                new BridgeRoleSyncRequest(link.discordUserId(), statuses)).ifPresentOrElse(result -> {
                    if (result.success()) {
                        lastSyncedStatuses.put(link.twitchUserId(), statuses);
                    } else {
                        LOGGER.warn("Rollen-Sync fuer Discord-Nutzer {} fehlgeschlagen: {}", link.discordUserId(), result.message());
                    }
                }, () -> LOGGER.warn("Discord-Bridge nicht erreichbar, Rollen-Sync uebersprungen."));
    }
}
