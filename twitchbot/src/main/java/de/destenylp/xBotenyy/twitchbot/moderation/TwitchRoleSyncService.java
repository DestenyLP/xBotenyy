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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class TwitchRoleSyncService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TwitchRoleSyncService.class);
    private static final int RECONCILE_MODERATOR_REMOVAL_CONFIRMATIONS = 2;
    private final AccountLinkRepository accountLinkRepository;
    private final ModerationBridgeClient bridgeClient;
    private final Supplier<BridgeSettings> bridgeSettingsSupplier;
    private final Map<String, List<TwitchRoleSyncStatus>> lastSyncedStatuses = new ConcurrentHashMap<>();
    private final Map<String, Integer> reconcileModeratorMissStreak = new ConcurrentHashMap<>();

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
        Optional<Set<String>> subscribersOpt = moderationApiClient.getSubscriberUserIds(broadcasterId);
        Optional<Set<String>> vipsOpt = moderationApiClient.getVipUserIds(broadcasterId);
        Optional<Set<String>> moderatorsOpt = moderationApiClient.getModeratorUserIds(broadcasterId);
        if (subscribersOpt.isEmpty() || vipsOpt.isEmpty() || moderatorsOpt.isEmpty()) {
            LOGGER.warn("Skipping periodic role reconciliation for {} because at least one Twitch API query failed; "
                    + "roles are left unchanged to avoid incorrectly removing them.", channelLogin);
            return;
        }
        Set<String> subscribers = subscribersOpt.get();
        Set<String> vips = vipsOpt.get();
        Set<String> moderators = moderatorsOpt.get();
        for (AccountLink link : accountLinkRepository.findAll()) {
            boolean isBroadcaster = link.twitchUserId().equals(broadcasterId);

            boolean isModeratorNow = isBroadcaster || moderators.contains(link.twitchUserId());
            boolean wasModeratorBefore = lastKnownStatuses(link.twitchUserId()).contains(TwitchRoleSyncStatus.MODERATOR);
            boolean keepModerator;
            if (isModeratorNow) {
                reconcileModeratorMissStreak.remove(link.twitchUserId());
                keepModerator = true;
            } else if (wasModeratorBefore) {

                int misses = reconcileModeratorMissStreak.merge(link.twitchUserId(), 1, Integer::sum);
                keepModerator = misses < RECONCILE_MODERATOR_REMOVAL_CONFIRMATIONS;
                if (!keepModerator) {
                    reconcileModeratorMissStreak.remove(link.twitchUserId());
                }
            } else {
                keepModerator = false;
            }
            List<TwitchRoleSyncStatus> statuses = new ArrayList<>();
            if (isBroadcaster) {
                statuses.add(TwitchRoleSyncStatus.BROADCASTER);
            }
            if (keepModerator) {
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

    private List<TwitchRoleSyncStatus> lastKnownStatuses(String twitchUserId) {
        List<TwitchRoleSyncStatus> known = lastSyncedStatuses.get(twitchUserId);
        return known != null ? known : List.of();
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

        if (previous != null && java.util.Set.copyOf(previous).equals(java.util.Set.copyOf(statuses))) {
            return;
        }
        BridgeSettings settings = bridgeSettingsSupplier.get();
        bridgeClient.sendRoleSync(settings,
                new BridgeRoleSyncRequest(link.discordUserId(), statuses)).ifPresentOrElse(result -> {
            if (result.success()) {
                lastSyncedStatuses.put(link.twitchUserId(), statuses);
            } else {
                LOGGER.warn("Role sync for Discord user {} failed: {}", link.discordUserId(), result.message());
            }
        }, () -> LOGGER.warn("Discord bridge unreachable, role sync skipped."));
    }
}

