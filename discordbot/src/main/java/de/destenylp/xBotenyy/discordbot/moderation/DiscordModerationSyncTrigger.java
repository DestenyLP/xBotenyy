package de.destenylp.xBotenyy.discordbot.moderation;

import de.destenylp.xBotenyy.common.moderation.AccountLink;
import de.destenylp.xBotenyy.common.moderation.AccountLinkRepository;
import de.destenylp.xBotenyy.common.moderation.ModerationAction;
import de.destenylp.xBotenyy.common.moderation.bridge.BridgeActionRequest;
import de.destenylp.xBotenyy.common.moderation.bridge.BridgeSettings;
import de.destenylp.xBotenyy.common.moderation.bridge.ModerationBridgeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Supplier;

public class DiscordModerationSyncTrigger {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordModerationSyncTrigger.class);

    private final AccountLinkRepository accountLinkRepository;
    private final ModerationBridgeClient bridgeClient;
    private final Supplier<BridgeSettings> bridgeSettingsSupplier;

    public DiscordModerationSyncTrigger(AccountLinkRepository accountLinkRepository, ModerationBridgeClient bridgeClient,
                                        Supplier<BridgeSettings> bridgeSettingsSupplier) {
        this.accountLinkRepository = accountLinkRepository;
        this.bridgeClient = bridgeClient;
        this.bridgeSettingsSupplier = bridgeSettingsSupplier;
    }

    public void trigger(String discordUserId, ModerationAction action, String reason, long durationSeconds,
                        String sourceModeratorName) {
        BridgeSettings settings = bridgeSettingsSupplier.get();
        if (!settings.isPeerConfigured()) {
            return;
        }
        Optional<AccountLink> linkOpt = accountLinkRepository.findByDiscordUserId(discordUserId);
        if (linkOpt.isEmpty()) {
            return;
        }
        AccountLink link = linkOpt.get();
        BridgeActionRequest request = new BridgeActionRequest(link.twitchUserId(), link.twitchLogin(), action, reason,
                durationSeconds, sourceModeratorName);
        bridgeClient.sendAction(settings.peerUrl(), settings.token(), request).ifPresentOrElse(result -> {
            if (!result.success()) {
                LOGGER.warn("Sync nach Twitch fuer {} fehlgeschlagen: {}", link.twitchLogin(), result.message());
            }
        }, () -> LOGGER.warn("Twitch-Bridge unter {} nicht erreichbar, Sync uebersprungen.", settings.peerUrl()));
    }
}
