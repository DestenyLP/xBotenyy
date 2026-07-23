package de.destenylp.xBotenyy.discordbot.tickets;

import de.destenylp.xBotenyy.discordbot.core.AbstractEmbedFactory;
import de.destenylp.xBotenyy.discordbot.util.DiscordColors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.util.List;

public final class TicketEmbedFactory extends AbstractEmbedFactory {
    private TicketEmbedFactory() {
    }

    public static MessageEmbed buildPanelEmbed() {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(DiscordColors.brand());
        eb.setTitle("\uD83C\uDFAB Support-Ticket erstellen");
        eb.setDescription("Wähle unten eine Kategorie aus, um ein neues Ticket zu eröffnen. " +
                "Ein privater Kanal wird erstellt, in dem dir das Team weiterhilft.");
        return eb.build();
    }

    public static List<ActionRow> buildPanelComponents() {
        StringSelectMenu.Builder menu = StringSelectMenu.create("ticket:panel:category")
                .setPlaceholder("Kategorie auswählen...");
        for (TicketCategory category : TicketCategory.values()) {
            menu.addOptions(SelectOption.of(category.getLabel(), category.getKey())
                    .withDescription(category.getDescription())
                    .withEmoji(Emoji.fromUnicode(category.getEmoji())));
        }
        return List.of(ActionRow.of(menu.build()));
    }

    public static MessageEmbed buildTicketEmbed(Ticket ticket) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(ticket.getStatus() == TicketStatus.CLOSED ? ticket.getStatus().getColor() : ticket.getPriority().getColor());
        eb.setTitle(ticket.getCategory().getEmoji() + " Ticket #" + ticket.getId() + " – " + ticket.getSubject());
        eb.setDescription(ticket.getDescription() == null || ticket.getDescription().isBlank()
                ? "_Keine weitere Beschreibung angegeben._" : ticket.getDescription());
        eb.addField("Kategorie", ticket.getCategory().getLabel(), true);
        eb.addField("Priorität", ticket.getPriority().getEmoji() + " " + ticket.getPriority().getLabel(), true);
        eb.addField("Status", statusLine(ticket.getStatus()), true);
        eb.addField("Erstellt von", "<@" + ticket.getAuthorId() + "> (" + ticket.getAuthorName() + ")", true);

        if (ticket.getClaimedByName() != null) {
            eb.addField("Bearbeitet von", "<@" + ticket.getClaimedById() + "> (" + ticket.getClaimedByName() + ")", true);
        }
        if (!ticket.getParticipantIds().isEmpty()) {
            String participants = ticket.getParticipantIds().stream()
                    .map(id -> "<@" + id + ">")
                    .reduce((a, b) -> a + " " + b)
                    .orElse("-");
            eb.addField("Weitere Teilnehmer", participants, false);
        }
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            eb.addField("Geschlossen von", ticket.getClosedByName() != null
                    ? "<@" + ticket.getClosedById() + "> (" + ticket.getClosedByName() + ")" : "Automatisch (Inaktivität)", true);
            if (ticket.getCloseReason() != null && !ticket.getCloseReason().isBlank()) {
                eb.addField("Grund", ticket.getCloseReason(), false);
            }
        }

        appendCreatedIdFooter(eb, ticket.getCreatedAt(), ticket.getId());
        return eb.build();
    }

    public static List<ActionRow> buildTicketComponents(Ticket ticket) {
        boolean closed = ticket.getStatus().isClosed();
        boolean claimed = ticket.getStatus() == TicketStatus.CLAIMED;

        Button claim = claimed
                ? Button.secondary("ticket:unclaim:" + ticket.getId(), "Freigeben")
                : Button.primary("ticket:claim:" + ticket.getId(), "Übernehmen");
        claim = claim.withDisabled(closed);

        Button close = Button.danger("ticket:close:" + ticket.getId(), "Schließen").withDisabled(closed);

        StringSelectMenu.Builder prioMenu = StringSelectMenu.create("ticket:priority:" + ticket.getId())
                .setPlaceholder("Priorität ändern...")
                .setDisabled(closed);
        for (TicketPriority priority : TicketPriority.values()) {
            prioMenu.addOptions(SelectOption.of(priority.getLabel(), priority.getKey())
                    .withEmoji(Emoji.fromUnicode(priority.getEmoji()))
                    .withDefault(priority == ticket.getPriority()));
        }

        return List.of(ActionRow.of(claim, close), ActionRow.of(prioMenu.build()));
    }

    public static MessageEmbed buildLogEmbed(Ticket ticket) {
        EmbedBuilder eb = statusEmbed(TicketStatus.CLOSED);
        eb.setTitle("\uD83D\uDCC1 Ticket #" + ticket.getId() + " geschlossen – " + ticket.getSubject());
        eb.addField("Kategorie", ticket.getCategory().getLabel(), true);
        eb.addField("Priorität", ticket.getPriority().getEmoji() + " " + ticket.getPriority().getLabel(), true);
        eb.addField("Erstellt von", "<@" + ticket.getAuthorId() + "> (" + ticket.getAuthorName() + ")", true);
        eb.addField("Geschlossen von", ticket.getClosedByName() != null
                ? "<@" + ticket.getClosedById() + "> (" + ticket.getClosedByName() + ")" : "Automatisch (Inaktivität)", true);
        if (ticket.getCloseReason() != null && !ticket.getCloseReason().isBlank()) {
            eb.addField("Grund", ticket.getCloseReason(), false);
        }
        if (ticket.getRatingScore() != null) {
            eb.addField("Bewertung", "\u2B50".repeat(ticket.getRatingScore()) + " (" + ticket.getRatingScore() + "/5)", true);
            if (ticket.getRatingComment() != null && !ticket.getRatingComment().isBlank()) {
                eb.addField("Kommentar zur Bewertung", ticket.getRatingComment(), false);
            }
        }
        long durationMinutes = (ticket.getClosedAt() - ticket.getCreatedAt()) / 60000;
        eb.addField("Bearbeitungsdauer", formatDuration(durationMinutes), true);
        appendCreatedIdFooter(eb, ticket.getCreatedAt(), ticket.getId());
        return eb.build();
    }

    private static String formatDuration(long minutes) {
        if (minutes < 60) {
            return minutes + " Minute(n)";
        }
        long hours = minutes / 60;
        long rest = minutes % 60;
        if (hours < 24) {
            return hours + "h " + rest + "min";
        }
        long days = hours / 24;
        return days + "d " + (hours % 24) + "h";
    }

    public static MessageEmbed buildMemberOverviewEmbed(List<Ticket> tickets) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(DiscordColors.brand());
        eb.setTitle("\uD83C\uDFAB Deine Tickets");

        if (tickets.isEmpty()) {
            eb.setDescription("Du hast bisher noch keine Tickets erstellt.");
            return eb.build();
        }

        eb.setDescription("Übersicht über alle von dir erstellten Tickets.");
        for (Ticket ticket : tickets) {
            String value = boldStatusLine(ticket.getStatus()) + "\n" +
                    "Kategorie: " + ticket.getCategory().getLabel() + "\n" +
                    createdLine(ticket.getCreatedAt());
            eb.addField("#" + ticket.getId() + " · " + ticket.getSubject(), value, false);
        }
        return eb.build();
    }

    public static MessageEmbed buildRatingRequestEmbed(Ticket ticket) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(DiscordColors.brand());
        eb.setTitle("\u2B50 Wie war unser Support?");
        eb.setDescription("Dein Ticket **#" + ticket.getId() + " · " + ticket.getSubject() + "** wurde geschlossen. " +
                "Wir würden uns über eine kurze Bewertung freuen!");
        return eb.build();
    }

    public static List<ActionRow> buildRatingComponents(Ticket ticket) {
        Button[] stars = new Button[5];
        for (int i = 1; i <= 5; i++) {
            stars[i - 1] = Button.secondary("ticket:rate:" + ticket.getGuildId() + ":" + ticket.getId() + ":" + i, "\u2B50".repeat(i));
        }
        return List.of(ActionRow.of(List.of(stars)));
    }

    public static MessageEmbed buildOverviewEmbed(String guildName, List<Ticket> openTickets) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(DiscordColors.brand());
        eb.setTitle("\uD83C\uDFAB Offene Tickets – " + guildName);

        if (openTickets.isEmpty()) {
            eb.setDescription("Aktuell sind keine Tickets offen. \uD83C\uDF89");
            return eb.build();
        }

        eb.setDescription(openTickets.size() + " offene(s) Ticket(s).");
        openTickets.stream()
                .sorted((a, b) -> b.getPriority().ordinal() - a.getPriority().ordinal())
                .limit(25)
                .forEach(ticket -> {
                    String value = ticket.getStatus().getEmoji() + " " + ticket.getStatus().getLabel() + " · " +
                            ticket.getPriority().getEmoji() + " " + ticket.getPriority().getLabel() + "\n" +
                            "Ersteller: <@" + ticket.getAuthorId() + ">" +
                            (ticket.getChannelId() != null ? " · <#" + ticket.getChannelId() + ">" : "");
                    eb.addField("#" + ticket.getId() + " · " + ticket.getSubject(), value, false);
                });
        return eb.build();
    }
}
