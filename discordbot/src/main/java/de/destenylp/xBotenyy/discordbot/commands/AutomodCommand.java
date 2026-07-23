package de.destenylp.xBotenyy.discordbot.commands;

import de.destenylp.xBotenyy.discordbot.automod.AutomodService;
import de.destenylp.xBotenyy.common.automod.AutomodSettings;
import de.destenylp.xBotenyy.common.automod.AutomodVerdict;
import de.destenylp.xBotenyy.discordbot.core.AbstractGuildCommand;
import de.destenylp.xBotenyy.discordbot.util.PermissionGuard;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.Optional;

public class AutomodCommand extends AbstractGuildCommand {
    private final AutomodService service;

    public AutomodCommand(AutomodService service) {
        this.service = service;
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("automod", "AutoMod-Diagnose (Konfiguration erfolgt ausschließlich über discordbot.properties)")
                .addSubcommands(
                        new SubcommandData("status", "Zeigt die aktuelle AutoMod-Konfiguration"),
                        new SubcommandData("test", "Prüft einen Text gegen die Filter, ohne eine echte Nachricht zu senden")
                                .addOption(OptionType.STRING, "text", "Zu prüfender Text", true));
    }

    @Override
    protected void executeInGuild(SlashCommandInteractionEvent event, Guild guild, String subcommand) {
        if (!PermissionGuard.requireManageServer(event)) {
            return;
        }
        switch (subcommand) {
            case "status" -> handleStatus(event);
            case "test" -> handleTest(event);
            default -> replyUnknownSubcommand(event);
        }
    }

    private void handleStatus(SlashCommandInteractionEvent event) {
        AutomodSettings settings = service.getSettings();
        StringBuilder sb = new StringBuilder();
        sb.append("**AutoMod-Status**\n");
        sb.append("Gesamtstatus: ").append(settings.isEnabled() ? "✅ Aktiviert" : "❌ Deaktiviert").append("\n");
        sb.append("Log-Kanal: ").append(describeChannel(settings.getLogChannelId())).append("\n");
        sb.append("Bypass für `Server verwalten`: ").append(settings.isBypassManageServer() ? "Ja" : "Nein").append("\n");
        sb.append("Bypass für `Administrator` (betrifft auch den Server-Owner): ").append(settings.isBypassAdministrator() ? "Ja" : "Nein").append("\n");
        sb.append("Ausgenommene Rollen: ").append(settings.getExemptRoleIds().isEmpty() ? "Keine" : settings.getExemptRoleIds().size()).append("\n");
        sb.append("Ausgenommene Kanäle: ").append(settings.getExemptChannelIds().isEmpty() ? "Keine" : settings.getExemptChannelIds().size()).append("\n\n");

        sb.append("Verbotene Wörter: ").append(ruleLine(settings.getWordFilter().isEnabled(), settings.getWordFilter().getAction().getLabel())).append("\n");
        sb.append("Invite-Links: ").append(ruleLine(settings.getInviteFilter().enabled(), settings.getInviteFilter().action().getLabel())).append("\n");
        sb.append("Massen-Erwähnungen: ").append(ruleLine(settings.getMentionFilter().enabled(), settings.getMentionFilter().action().getLabel()
                + ", Grenze: " + settings.getMentionFilter().maxMentions())).append("\n");
        sb.append("Großschreibung: ").append(ruleLine(settings.getCapsFilter().enabled(), settings.getCapsFilter().action().getLabel()
                + ", ab " + settings.getCapsFilter().minLength() + " Zeichen, max. " + settings.getCapsFilter().maxPercentage() + "%")).append("\n");
        sb.append("Spam (Rate): ").append(ruleLine(settings.getSpamFilter().enabled(), settings.getSpamFilter().action().getLabel()
                + ", max. " + settings.getSpamFilter().maxMessages() + " Nachrichten/" + settings.getSpamFilter().windowSeconds() + "s")).append("\n");
        sb.append("Wiederholte Nachrichten: ").append(ruleLine(settings.getDuplicateFilter().enabled(), settings.getDuplicateFilter().action().getLabel()
                + ", ab " + settings.getDuplicateFilter().maxRepeats() + "x")).append("\n");
        sb.append("Nicht erlaubte Links: ").append(ruleLine(settings.getLinkFilter().enabled(), settings.getLinkFilter().action().getLabel())).append("\n");
        sb.append("KI-Toxizitätserkennung: ").append(ruleLine(settings.getAiFilter().enabled(), settings.getAiFilter().action().getLabel()
                + ", Schwelle " + settings.getAiFilter().threshold())).append(service.isAiAvailable() ? "" : " ⚠️ Kein API-Key hinterlegt").append("\n\n");

        sb.append("Strike-Eskalation: ").append(settings.getStrikeConfig().enabled() ? "Aktiviert" : "Deaktiviert").append("\n");
        if (settings.getStrikeConfig().enabled()) {
            sb.append("Timeout ab ").append(settings.getStrikeConfig().timeoutThreshold()).append(" Strikes (")
                    .append(settings.getStrikeConfig().timeoutDurationMinutes()).append(" min), Kick ab ")
                    .append(settings.getStrikeConfig().kickThreshold()).append(", Bann ab ")
                    .append(settings.getStrikeConfig().banThreshold()).append(", Ablauf nach ")
                    .append(settings.getStrikeConfig().expiryMinutes()).append(" min\n");
        }

        event.reply(sb.toString()).setEphemeral(true).queue();
    }

    private void handleTest(SlashCommandInteractionEvent event) {
        String text = event.getOption("text").getAsString();
        Optional<AutomodVerdict> verdict = service.evaluateTextOnly(text);
        if (verdict.isEmpty()) {
            event.reply("✅ Kein Regelverstoß gefunden (Spam-, Wiederholungs- und KI-Prüfung werden im Test-Modus nicht ausgewertet).")
                    .setEphemeral(true).queue();
            return;
        }
        AutomodVerdict result = verdict.get();
        event.reply("🚫 Regel ausgelöst: **" + result.ruleType().getLabel() + "**\nGrund: " + result.reason()
                + "\nKonfigurierte Aktion: " + result.action().getLabel()).setEphemeral(true).queue();
    }

    private String ruleLine(boolean enabled, String detail) {
        return (enabled ? "✅ Aktiviert" : "❌ Deaktiviert") + (enabled ? " (" + detail + ")" : "");
    }

    private String describeChannel(String channelId) {
        return channelId == null || channelId.isBlank() ? "Nicht gesetzt" : "<#" + channelId + ">";
    }
}
