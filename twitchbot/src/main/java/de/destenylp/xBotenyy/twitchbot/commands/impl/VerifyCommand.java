package de.destenylp.xBotenyy.twitchbot.commands.impl;

import de.destenylp.xBotenyy.common.commands.CommandPermission;
import de.destenylp.xBotenyy.common.moderation.AccountLinkRepository;
import de.destenylp.xBotenyy.common.moderation.bridge.BridgeLinkConfirmRequest;
import de.destenylp.xBotenyy.common.moderation.bridge.BridgeLinkConfirmResult;
import de.destenylp.xBotenyy.common.moderation.bridge.BridgeSettings;
import de.destenylp.xBotenyy.common.moderation.bridge.ModerationBridgeClient;
import de.destenylp.xBotenyy.twitchbot.commands.AbstractTwitchCommand;
import de.destenylp.xBotenyy.twitchbot.commands.TwitchCommandContext;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class VerifyCommand extends AbstractTwitchCommand {
    private static final String USAGE = "Nutzung: !verify <code>";

    private final AccountLinkRepository accountLinkRepository;
    private final ModerationBridgeClient bridgeClient;
    private final Supplier<BridgeSettings> bridgeSettingsSupplier;

    public VerifyCommand(AccountLinkRepository accountLinkRepository, ModerationBridgeClient bridgeClient,
                          Supplier<BridgeSettings> bridgeSettingsSupplier) {
        super("verify", "Bestaetigt einen auf Discord mit /link erzeugten Code.", List.of(), CommandPermission.EVERYONE, 10);
        this.accountLinkRepository = accountLinkRepository;
        this.bridgeClient = bridgeClient;
        this.bridgeSettingsSupplier = bridgeSettingsSupplier;
    }

    @Override
    public void execute(TwitchCommandContext context) {
        if (context.arg(0) == null) {
            context.reply(USAGE);
            return;
        }
        BridgeSettings settings = bridgeSettingsSupplier.get();
        if (!settings.isPeerConfigured()) {
            context.reply("Die Verbindung zum Discord-Bot ist nicht konfiguriert.");
            return;
        }
        String code = context.arg(0).trim().toUpperCase();
        Optional<BridgeLinkConfirmResult> resultOpt = bridgeClient.sendLinkConfirm(settings,
                new BridgeLinkConfirmRequest(code, context.message().userId(), context.message().userLogin(), null, null));
        if (resultOpt.isEmpty() || !resultOpt.get().success()) {
            String reason = resultOpt.map(BridgeLinkConfirmResult::message).orElse("Discord-Bot nicht erreichbar.");
            context.reply("Verknuepfung fehlgeschlagen: " + reason);
            return;
        }
        BridgeLinkConfirmResult result = resultOpt.get();
        accountLinkRepository.save(result.discordUserId(), context.message().userId(), context.message().userLogin());
        context.reply("@" + context.message().displayName() + " erfolgreich mit Discord-Account "
                + result.discordUsername() + " verknuepft!");
    }
}
