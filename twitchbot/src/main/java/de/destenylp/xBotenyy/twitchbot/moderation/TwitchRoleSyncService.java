package de.destenylp.xBotenyy.twitchbot.moderation;

import de.destenylp.xBotenyy.common.moderation.AccountLink;
import de.destenylp.xBotenyy.common.moderation.AccountLinkRepository;
import de.destenylp.xBotenyy.common.moderation.TwitchRoleSyncStatus;
import de.destenylp.xBotenyy.common.moderation.bridge.BridgeRoleSyncRequest;
import de.destenylp.xBotenyy.common.moderation.bridge.BridgeSettings;
import de.destenylp.xBotenyy.common.moderation.bridge.ModerationBridgeClient;
import de.destenylp.xBotenyy.twitchbot.chat.TwitchChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

        List<TwitchRoleSyncStatus> previous = lastSyncedStatuses.get(message.userId());
        if (previous != null && previous.equals(statuses)) {
            return;
        }

        AccountLink link = linkOpt.get();
        bridgeClient.sendRoleSync(settings.peerUrl(), settings.token(),
                new BridgeRoleSyncRequest(link.discordUserId(), statuses)).ifPresentOrElse(result -> {
            if (result.success()) {
                lastSyncedStatuses.put(message.userId(), statuses);
            } else {
                LOGGER.warn("Rollen-Sync fuer Discord-Nutzer {} fehlgeschlagen: {}", link.discordUserId(), result.message());
            }
        }, () -> LOGGER.warn("Discord-Bridge nicht erreichbar, Rollen-Sync uebersprungen."));
    }
}
