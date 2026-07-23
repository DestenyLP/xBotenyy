package de.destenylp.xBotenyy.discordbot.commands.report;

import de.destenylp.xBotenyy.discordbot.core.AbstractGuildCommand;
import de.destenylp.xBotenyy.discordbot.reports.Report;
import de.destenylp.xBotenyy.discordbot.reports.ReportEmbedFactory;
import de.destenylp.xBotenyy.discordbot.reports.ReportService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class MyReportsCommand extends AbstractGuildCommand {
    private final ReportService service;

    public MyReportsCommand(ReportService service) {
        this.service = service;
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("reports", "Zeigt deine eingereichten Reports und deren Status")
                .addOption(OptionType.STRING, "id", "ID eines bestimmten Reports für die Detailansicht", false);
    }

    @Override
    protected boolean hasSubcommands() {
        return false;
    }

    @Override
    protected void executeInGuild(SlashCommandInteractionEvent event, Guild guild, String subcommand) {
        OptionMapping idOption = event.getOption("id");

        if (idOption != null) {
            Optional<Report> reportOpt = service.getReport(guild.getId(), idOption.getAsString());
            if (reportOpt.isEmpty() || !reportOpt.get().getReporterId().equals(event.getUser().getId())) {
                event.reply("Es wurde kein Report mit dieser ID gefunden, den du eingereicht hast.").setEphemeral(true).queue();
                return;
            }
            event.replyEmbeds(ReportEmbedFactory.buildMemberDetailEmbed(reportOpt.get())).setEphemeral(true).queue();
            return;
        }

        List<Report> reports = service.getReportsByMember(guild.getId(), event.getUser().getId()).stream()
                .sorted(Comparator.comparingLong(Report::getCreatedAt).reversed())
                .toList();

        event.replyEmbeds(ReportEmbedFactory.buildMemberOverviewEmbed(reports)).setEphemeral(true).queue();
    }
}
