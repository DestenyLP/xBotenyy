package de.destenylp.xBotenyy.discordbot.giveaways;

import de.destenylp.xBotenyy.discordbot.core.AbstractEmbedFactory;
import de.destenylp.xBotenyy.discordbot.util.DiscordColors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.util.List;

public final class GiveawayEmbedFactory extends AbstractEmbedFactory {
    private GiveawayEmbedFactory() {
    }

    public static MessageEmbed buildAnnouncementEmbed(Giveaway giveaway) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(giveaway.getStatus().getColor());
        eb.setTitle(giveaway.getStatus().getEmoji() + " Gewinnspiel: " + giveaway.getPrize());
        eb.setDescription(giveaway.getDescription() == null || giveaway.getDescription().isBlank()
                ? "Klicke auf den Button, um teilzunehmen!" : giveaway.getDescription());

        long endSeconds = giveaway.getEndAt() / 1000;
        eb.addField("Endet", "<t:" + endSeconds + ":R> (<t:" + endSeconds + ":f>)", true);
        eb.addField("Gewinner", giveaway.getWinnerCount() + "x", true);
        eb.addField("Gehostet von", "<@" + giveaway.getHostId() + ">", true);
        if (giveaway.getRequiredRoleId() != null) {
            eb.addField("Voraussetzung", "<@&" + giveaway.getRequiredRoleId() + ">", true);
        }
        eb.addField("Teilnehmer", String.valueOf(giveaway.getParticipantIds().size()), true);

        appendCreatedIdFooter(eb, giveaway.getCreatedAt(), giveaway.getId());
        return eb.build();
    }

    public static List<ActionRow> buildEnterComponents(Giveaway giveaway) {
        Button button = Button.primary("giveaway:enter:" + giveaway.getId(),
                        "Teilnehmen (" + giveaway.getParticipantIds().size() + ")")
                .withEmoji(Emoji.fromUnicode("\uD83C\uDF89"))
                .withDisabled(!giveaway.isRunning());
        return List.of(ActionRow.of(button));
    }

    public static MessageEmbed buildEndedEmbed(Giveaway giveaway) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(giveaway.getStatus().getColor());
        eb.setTitle(giveaway.getStatus().getEmoji() + " Gewinnspiel beendet: " + giveaway.getPrize());
        eb.setDescription(describeWinners(giveaway));
        eb.addField("Teilnehmer", String.valueOf(giveaway.getParticipantIds().size()), true);
        eb.addField("Gehostet von", "<@" + giveaway.getHostId() + ">", true);
        appendCreatedIdFooter(eb, giveaway.getCreatedAt(), giveaway.getId());
        return eb.build();
    }

    public static MessageEmbed buildCancelledEmbed(Giveaway giveaway) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(giveaway.getStatus().getColor());
        eb.setTitle(giveaway.getStatus().getEmoji() + " Gewinnspiel abgebrochen: " + giveaway.getPrize());
        eb.setDescription("Dieses Gewinnspiel wurde vorzeitig abgebrochen.");
        appendCreatedIdFooter(eb, giveaway.getCreatedAt(), giveaway.getId());
        return eb.build();
    }

    private static String describeWinners(Giveaway giveaway) {
        if (giveaway.getWinnerIds().isEmpty()) {
            return "Niemand hat an diesem Gewinnspiel teilgenommen.";
        }
        String mentions = giveaway.getWinnerIds().stream()
                .map(id -> "<@" + id + ">")
                .reduce((a, b) -> a + ", " + b)
                .orElse("-");
        return "Gewinner: " + mentions;
    }

    public static String buildWinnerAnnouncementContent(Giveaway giveaway) {
        if (giveaway.getWinnerIds().isEmpty()) {
            return "\uD83D\uDE22 Niemand hat an dem Gewinnspiel für **" + giveaway.getPrize() + "** teilgenommen.";
        }
        String mentions = giveaway.getWinnerIds().stream()
                .map(id -> "<@" + id + ">")
                .reduce((a, b) -> a + " " + b)
                .orElse("-");
        return "\uD83C\uDF89 Herzlichen Glückwunsch " + mentions + "! Ihr habt **" + giveaway.getPrize() + "** gewonnen!";
    }

    public static MessageEmbed buildListEmbed(String guildName, List<Giveaway> runningGiveaways) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(DiscordColors.brand());
        eb.setTitle("\uD83C\uDF89 Laufende Gewinnspiele – " + guildName);

        if (runningGiveaways.isEmpty()) {
            eb.setDescription("Aktuell laufen keine Gewinnspiele.");
            return eb.build();
        }

        eb.setDescription(runningGiveaways.size() + " laufende(s) Gewinnspiel(e).");
        runningGiveaways.stream()
                .sorted((a, b) -> Long.compare(a.getEndAt(), b.getEndAt()))
                .limit(25)
                .forEach(giveaway -> {
                    long endSeconds = giveaway.getEndAt() / 1000;
                    String value = "Endet <t:" + endSeconds + ":R>\n" +
                            "Gewinner: " + giveaway.getWinnerCount() + "x · Teilnehmer: " + giveaway.getParticipantIds().size() + "\n" +
                            "Gehostet von <@" + giveaway.getHostId() + ">" +
                            (giveaway.getChannelId() != null ? " · <#" + giveaway.getChannelId() + ">" : "");
                    eb.addField("#" + giveaway.getId() + " · " + giveaway.getPrize(), value, false);
                });
        return eb.build();
    }
}
