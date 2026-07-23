package de.destenylp.xBotenyy.discordbot.reports;

import de.destenylp.xBotenyy.discordbot.core.AbstractEmbedFactory;
import de.destenylp.xBotenyy.discordbot.util.DiscordColors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.List;

public final class ReportEmbedFactory extends AbstractEmbedFactory {
    private ReportEmbedFactory() {
    }

    public static MessageEmbed buildChannelEmbed(Report report) {
        EmbedBuilder eb = statusEmbed(report.getStatus());
        eb.setTitle(report.getCategory().getEmoji() + " Report #" + report.getId() + " – " + report.getSubject());
        eb.setDescription(report.getDescription());
        eb.addField("Kategorie", report.getCategory().getLabel(), true);
        eb.addField("Status", statusLine(report.getStatus()), true);
        eb.addField("Gemeldet von", "<@" + report.getReporterId() + "> (" + report.getReporterName() + ")", true);

        if (report.getTarget() != null && !report.getTarget().isBlank()) {
            eb.addField("Betroffen", report.getTarget(), true);
        }
        if (report.getAssignedModName() != null) {
            eb.addField("Bearbeitet von", "<@" + report.getAssignedModId() + "> (" + report.getAssignedModName() + ")", true);
        }
        if (report.getEvidence() != null && !report.getEvidence().isBlank()) {
            eb.addField("Beweise / Kontext", report.getEvidence(), false);
        }
        if (report.getStatus() == ReportStatus.RESOLVED && report.getResolutionNote() != null && !report.getResolutionNote().isBlank()) {
            eb.addField("Abschlussnotiz", report.getResolutionNote(), false);
        }
        if (report.getStatus() == ReportStatus.REJECTED && report.getRejectionReason() != null) {
            eb.addField("Ablehnungsgrund", report.getRejectionReason(), false);
        }

        appendCreatedIdFooter(eb, report.getCreatedAt(), report.getId());
        return eb.build();
    }

    public static List<ActionRow> buildChannelComponents(Report report) {
        boolean closed = report.getStatus().isClosed();
        boolean claimed = report.getStatus() == ReportStatus.IN_PROGRESS;

        Button claim = claimed
                ? Button.secondary("report:claim:" + report.getId(), "In Bearbeitung von " + report.getAssignedModName())
                : Button.primary("report:claim:" + report.getId(), "Übernehmen");
        claim = claim.withDisabled(closed || claimed);

        Button resolve = Button.success("report:resolve:" + report.getId(), "Als erledigt markieren").withDisabled(closed);
        Button reject = Button.danger("report:reject:" + report.getId(), "Ablehnen").withDisabled(closed);

        return List.of(ActionRow.of(claim, resolve, reject));
    }

    public static MessageEmbed buildMemberDetailEmbed(Report report) {
        EmbedBuilder eb = statusEmbed(report.getStatus());
        eb.setTitle(report.getCategory().getEmoji() + " Report #" + report.getId());
        eb.addField("Betreff", report.getSubject(), false);
        eb.addField("Status", statusLine(report.getStatus()), true);
        eb.addField("Kategorie", report.getCategory().getLabel(), true);

        if (report.getAssignedModName() != null) {
            eb.addField("Bearbeitet von", report.getAssignedModName(), true);
        }

        eb.addField("Beschreibung", report.getDescription(), false);

        if (report.getStatus() == ReportStatus.REJECTED && report.getRejectionReason() != null) {
            eb.addField("Ablehnungsgrund", report.getRejectionReason(), false);
        }
        if (report.getStatus() == ReportStatus.RESOLVED && report.getResolutionNote() != null && !report.getResolutionNote().isBlank()) {
            eb.addField("Abschlussnotiz", report.getResolutionNote(), false);
        }

        appendSubmittedFooter(eb, report.getCreatedAt());
        return eb.build();
    }

    public static MessageEmbed buildMemberOverviewEmbed(List<Report> reports) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(DiscordColors.brand());
        eb.setTitle("\uD83D\uDCCB Deine Reports");

        if (reports.isEmpty()) {
            eb.setDescription("Du hast bisher noch keine Reports eingereicht. Nutze `/report send`, um einen neuen Report zu erstellen.");
            return eb.build();
        }

        eb.setDescription("Übersicht über alle von dir eingereichten Reports. Nutze `/reports id:<ID>` für die Detailansicht.");

        for (Report report : reports) {
            String value = boldStatusLine(report.getStatus()) + "\n" +
                    "Kategorie: " + report.getCategory().getLabel() + "\n" +
                    createdLine(report.getCreatedAt());
            eb.addField("#" + report.getId() + " · " + report.getSubject(), value, false);
        }

        return eb.build();
    }

    public static MessageEmbed buildRejectionDmEmbed(Report report) {
        EmbedBuilder eb = statusEmbed(ReportStatus.REJECTED);
        eb.setTitle("\u26AB Dein Report wurde abgelehnt");
        eb.addField("Report", "#" + report.getId() + " · " + report.getSubject(), false);
        eb.addField("Begründung", report.getRejectionReason(), false);
        eb.addField("Bearbeitet von", report.getAssignedModName(), false);
        eb.setFooter("Bei Rückfragen wende dich bitte an das Moderationsteam.");
        return eb.build();
    }

    public static MessageEmbed buildResolutionDmEmbed(Report report) {
        EmbedBuilder eb = statusEmbed(ReportStatus.RESOLVED);
        eb.setTitle("\uD83D\uDFE2 Dein Report wurde bearbeitet");
        eb.addField("Report", "#" + report.getId() + " · " + report.getSubject(), false);
        if (report.getResolutionNote() != null && !report.getResolutionNote().isBlank()) {
            eb.addField("Notiz vom Team", report.getResolutionNote(), false);
        }
        eb.addField("Bearbeitet von", report.getAssignedModName(), false);
        eb.setFooter("Danke für deine Meldung!");
        return eb.build();
    }
}
