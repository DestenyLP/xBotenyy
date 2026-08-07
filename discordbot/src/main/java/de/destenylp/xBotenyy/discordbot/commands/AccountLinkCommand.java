package de.destenylp.xBotenyy.discordbot.commands;

import de.destenylp.xBotenyy.common.moderation.AccountLink;
import de.destenylp.xBotenyy.common.moderation.bridge.BridgeLinkConfirmRequest;
import de.destenylp.xBotenyy.common.moderation.bridge.BridgeLinkConfirmResult;
import de.destenylp.xBotenyy.common.moderation.bridge.BridgeSettings;
import de.destenylp.xBotenyy.common.moderation.bridge.ModerationBridgeClient;
import de.destenylp.xBotenyy.common.util.AuditLog;
import de.destenylp.xBotenyy.discordbot.core.AbstractGuildCommand;
import de.destenylp.xBotenyy.discordbot.moderation.AccountLinkPanelFactory;
import de.destenylp.xBotenyy.discordbot.moderation.AccountLinkService;
import de.destenylp.xBotenyy.discordbot.moderation.ModerationPermissionGuard;
import de.destenylp.xBotenyy.discordbot.moderation.ModerationRoleSettingsRepository;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.Optional;
import java.util.function.Supplier;

public class AccountLinkCommand extends AbstractGuildCommand {
    private final AccountLinkService accountLinkService;
    private final ModerationBridgeClient bridgeClient;
    private final Supplier<BridgeSettings> bridgeSettingsSupplier;
    private final ModerationRoleSettingsRepository roleSettingsRepository;

    public AccountLinkCommand(AccountLinkService accountLinkService, ModerationBridgeClient bridgeClient,
                              Supplier<BridgeSettings> bridgeSettingsSupplier,
                              ModerationRoleSettingsRepository roleSettingsRepository) {
        this.accountLinkService = accountLinkService;
        this.bridgeClient = bridgeClient;
        this.bridgeSettingsSupplier = bridgeSettingsSupplier;
        this.roleSettingsRepository = roleSettingsRepository;
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("link", "Verknuepft deinen Discord- mit deinem Twitch-Account")
                .addSubcommands(
                        new SubcommandData("twitch", "Startet die Verknuepfung mit deinem Twitch-Account")
                                .addOptions(new OptionData(OptionType.STRING, "login", "Dein Twitch-Loginname", true)),
                        new SubcommandData("verify", "Bestaetigt einen auf Twitch mit !link erzeugten Code")
                                .addOptions(new OptionData(OptionType.STRING, "code", "Code aus !link auf Twitch", true)),
                        new SubcommandData("status", "Zeigt deinen aktuellen Verknuepfungsstatus"),
                        new SubcommandData("unlink", "Entfernt die Verknuepfung"),
                        new SubcommandData("panel", "Postet ein Verknuepfungs-Panel mit Button (Admin)")
                                .addOptions(new OptionData(OptionType.CHANNEL, "channel", "Zielkanal", true))
                );
    }

    @Override
    protected void executeInGuild(SlashCommandInteractionEvent event, Guild guild, String subcommand) {
        switch (subcommand) {
            case "twitch" -> handleTwitch(event);
            case "verify" -> handleVerify(event);
            case "status" -> handleStatus(event);
            case "unlink" -> handleUnlink(event);
            case "panel" -> handlePanel(event, guild);
            default -> replyUnknownSubcommand(event);
        }
    }

    private void handleTwitch(SlashCommandInteractionEvent event) {
        String login = event.getOption("login").getAsString().toLowerCase().replace("@", "");
        String code = accountLinkService.initiate(event.getUser().getId(), event.getUser().getName(), login);
        event.reply("Fast fertig! Poste **`!verify " + code + "`** innerhalb der naechsten 10 Minuten im Twitch-Chat "
                + "von **" + login + "**, um die Verknuepfung zu bestaetigen.").setEphemeral(true).queue();
        AuditLog.record(event.getGuild().getId(), event.getUser().getId(), "ACCOUNT_LINK_INITIATE", "twitch=" + login);
    }

    private void handleVerify(SlashCommandInteractionEvent event) {
        BridgeSettings settings = bridgeSettingsSupplier.get();
        if (!settings.isPeerConfigured()) {
            event.reply("Die Verbindung zum Twitch-Bot ist nicht konfiguriert. Bitte einen Admin kontaktieren.")
                    .setEphemeral(true).queue();
            return;
        }
        String code = event.getOption("code").getAsString().trim().toUpperCase();
        event.deferReply(true).queue();
        Optional<BridgeLinkConfirmResult> resultOpt = bridgeClient.sendLinkConfirm(settings,
                new BridgeLinkConfirmRequest(code, null, null, event.getUser().getId(), event.getUser().getName()));
        if (resultOpt.isEmpty() || !resultOpt.get().success()) {
            String reason = resultOpt.map(BridgeLinkConfirmResult::message).orElse("Twitch-Bot nicht erreichbar.");
            event.getHook().sendMessage("Verknuepfung fehlgeschlagen: " + reason).queue();
            return;
        }
        BridgeLinkConfirmResult result = resultOpt.get();
        event.getHook().sendMessage("\u2705 Erfolgreich mit Twitch-Account **" + result.twitchLogin() + "** verknuepft!").queue();
        AuditLog.record(event.getGuild().getId(), event.getUser().getId(), "ACCOUNT_LINK_CONFIRM",
                "twitch=" + result.twitchLogin());
    }

    private void handleStatus(SlashCommandInteractionEvent event) {
        Optional<AccountLink> linkOpt = accountLinkService.findLink(event.getUser().getId());
        if (linkOpt.isEmpty()) {
            event.reply("Du hast noch keinen Twitch-Account verknuepft. Nutze `/link twitch login:<name>`.")
                    .setEphemeral(true).queue();
            return;
        }
        event.reply("Verknuepft mit Twitch-Account **" + linkOpt.get().twitchLogin() + "**.").setEphemeral(true).queue();
    }

    private void handleUnlink(SlashCommandInteractionEvent event) {
        accountLinkService.unlink(event.getUser().getId());
        event.reply("Verknuepfung entfernt.").setEphemeral(true).queue();
        AuditLog.record(event.getGuild().getId(), event.getUser().getId(), "ACCOUNT_LINK_REMOVE", "");
    }

    private void handlePanel(SlashCommandInteractionEvent event, Guild guild) {
        if (!ModerationPermissionGuard.requireAdmin(event, roleSettingsRepository.getOrEmpty(guild.getId()))) {
            return;
        }
        TextChannel channel = event.getOption("channel").getAsChannel().asTextChannel();
        channel.sendMessageEmbeds(AccountLinkPanelFactory.buildPanelEmbed())
                .setComponents(AccountLinkPanelFactory.buildPanelComponents())
                .queue(unused -> {
                    event.reply("Panel wurde in " + channel.getAsMention() + " gepostet.").setEphemeral(true).queue();
                    AuditLog.record(guild.getId(), event.getUser().getId(), "ACCOUNT_LINK_PANEL", "channel=" + channel.getId());
                }, failure -> event.reply("Panel konnte nicht gepostet werden: " + failure.getMessage())
                        .setEphemeral(true).queue());
    }
}

