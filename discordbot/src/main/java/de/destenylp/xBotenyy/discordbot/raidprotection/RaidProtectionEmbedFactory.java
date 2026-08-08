package de.destenylp.xBotenyy.discordbot.raidprotection;

import de.destenylp.xBotenyy.discordbot.core.AbstractEmbedFactory;
import de.destenylp.xBotenyy.discordbot.util.DiscordColors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.Color;

public final class RaidProtectionEmbedFactory extends AbstractEmbedFactory {
    private RaidProtectionEmbedFactory() {
    }

    public static MessageEmbed buildAlertEmbed(String title, User user, String reason, RaidProtectionAction action) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(new Color(230, 30, 30));
        eb.setTitle("\uD83D\uDEE1\uFE0F " + title);
        if (user != null) {
            eb.addField("Nutzer", user.getAsMention() + " (`" + user.getId() + "`)", true);
            eb.addField("Account erstellt", "<t:" + user.getTimeCreated().toEpochSecond() + ":R>", true);
        }
        eb.addField("Aktion", action.name(), true);
        eb.addField("Grund", reason, false);
        timestampNow(eb);
        return eb.build();
    }

    public static MessageEmbed buildRaidModeEmbed(long durationSeconds, int currentJoinCount) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(DiscordColors.brand());
        eb.setTitle("\uD83D\uDEA8 Raid-Schutz aktiviert");
        eb.setDescription("Es wurden " + currentJoinCount + " Beitritte in kurzer Zeit erkannt. "
                + "Der Schutzmodus ist fuer " + durationSeconds + " Sekunden aktiv.");
        timestampNow(eb);
        return eb.build();
    }
}
