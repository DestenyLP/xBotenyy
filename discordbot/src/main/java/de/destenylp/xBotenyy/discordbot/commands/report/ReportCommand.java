package de.destenylp.xBotenyy.discordbot.commands.report;

import de.destenylp.xBotenyy.discordbot.core.AbstractGuildCommand;
import de.destenylp.xBotenyy.discordbot.reports.ReportCategory;
import de.destenylp.xBotenyy.discordbot.reports.ReportService;
import de.destenylp.xBotenyy.discordbot.reports.ReportSettings;
import de.destenylp.xBotenyy.discordbot.util.DiscordColors;
import de.destenylp.xBotenyy.common.util.AuditLog;
import de.destenylp.xBotenyy.discordbot.util.PermissionGuard;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportCommand extends AbstractGuildCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReportCommand.class);

    private final ReportService service;

    public ReportCommand(ReportService service) {
        this.service = service;
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("report", "Erstelle einen Report oder verwalte das Report-System")
                .addSubcommands(
                        new SubcommandData("send", "Startet den geführten Dialog zum Einreichen eines Reports"),
                        new SubcommandData("settings", "Konfiguriert den Report-Kanal und die Moderationsrolle (nur für das Team)")
                                .addOption(OptionType.CHANNEL, "channel", "Kanal, in den neue Reports gesendet werden", false)
                                .addOption(OptionType.ROLE, "role", "Rolle, die bei neuen Reports benachrichtigt wird und Reports bearbeiten darf", false)
                );
    }

    @Override
    protected void executeInGuild(SlashCommandInteractionEvent event, Guild guild, String subcommand) {
        switch (subcommand) {
            case "send" -> handleSend(event);
            case "settings" -> handleSettings(event);
            default -> replyUnknownSubcommand(event);
        }
    }

    private void handleSend(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        ReportSettings settings = service.getSettings(guild.getId()).orElse(null);

        if (settings == null || settings.getChannelId() == null) {
            event.reply("Das Report-System wurde auf diesem Server noch nicht eingerichtet. Bitte wende dich an das Team.").setEphemeral(true).queue();
            return;
        }

        StringSelectMenu.Builder menu = StringSelectMenu.create("report:category")
                .setPlaceholder("Wähle eine Kategorie für deinen Report");

        for (ReportCategory category : ReportCategory.values()) {
            menu.addOptions(SelectOption.of(category.getLabel(), category.getKey())
                    .withDescription(category.getDescription())
                    .withEmoji(Emoji.fromUnicode(category.getEmoji())));
        }

        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(DiscordColors.brand());
        eb.setTitle("\uD83D\uDCCB Neuen Report erstellen");
        eb.setDescription("Wähle unten eine Kategorie aus, um mit deinem Report zu starten. " +
                "Im Anschluss öffnet sich ein Formular, in dem du alle Details angeben kannst.");

        event.replyEmbeds(eb.build())
                .setComponents(ActionRow.of(menu.build()))
                .setEphemeral(true)
                .queue();
    }

    private void handleSettings(SlashCommandInteractionEvent event) {
        if (!PermissionGuard.requireManageServer(event)) {
            return;
        }

        Guild guild = event.getGuild();
        OptionMapping channelOption = event.getOption("channel");
        OptionMapping roleOption = event.getOption("role");

        if (channelOption == null && roleOption == null) {
            ReportSettings settings = service.getSettings(guild.getId()).orElse(null);
            event.reply(describeSettings(settings)).setEphemeral(true).queue();
            return;
        }

        if (channelOption != null) {
            TextChannel channel = channelOption.getAsChannel().asTextChannel();
            service.updateChannel(guild.getId(), channel.getId());
        }
        if (roleOption != null) {
            Role role = roleOption.getAsRole();
            service.updateNotifyRole(guild.getId(), role.getId());
        }

        ReportSettings updated = service.getSettings(guild.getId()).orElse(null);
        event.reply("Einstellungen aktualisiert!\n" + describeSettings(updated)).setEphemeral(true).queue();
        LOGGER.info("Report settings updated for guild {}", guild.getId());
        AuditLog.record(guild.getId(), event.getUser().getId(), "REPORT_SETTINGS_UPDATE",
                "channel=" + (updated != null ? updated.getChannelId() : null) + " role=" + (updated != null ? updated.getNotifyRoleId() : null));
    }

    private String describeSettings(ReportSettings settings) {
        if (settings == null || settings.getChannelId() == null) {
            return "Noch nicht konfiguriert. Nutze `/report settings channel:#kanal role:@Team`.";
        }
        return "Report-Kanal: <#" + settings.getChannelId() + ">\n" +
                "Moderationsrolle: " + (settings.getNotifyRoleId() != null ? "<@&" + settings.getNotifyRoleId() + ">" : "Nicht gesetzt") + "\n" +
                "Eingereichte Reports: " + settings.getReports().size();
    }
}
