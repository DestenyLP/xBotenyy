package de.destenylp.xBotenyy.discordbot.core;

import de.destenylp.xBotenyy.discordbot.util.DiscordTimestamps;
import net.dv8tion.jda.api.EmbedBuilder;

import java.time.Instant;

public abstract class AbstractEmbedFactory {
    protected static EmbedBuilder statusEmbed(EntityStatus status) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(status.getColor());
        return eb;
    }

    protected static String statusLine(EntityStatus status) {
        return status.getEmoji() + " " + status.getLabel();
    }

    protected static String boldStatusLine(EntityStatus status) {
        return status.getEmoji() + " **" + status.getLabel() + "**";
    }

    protected static String createdLine(long createdAt) {
        return "Erstellt: " + DiscordTimestamps.format(createdAt) + " Uhr";
    }

    protected static void appendCreatedIdFooter(EmbedBuilder eb, long createdAt, String id) {
        eb.setFooter("Erstellt am " + DiscordTimestamps.format(createdAt) + " Uhr · ID: " + id);
    }

    protected static void appendSubmittedFooter(EmbedBuilder eb, long createdAt) {
        eb.setFooter("Eingereicht am " + DiscordTimestamps.format(createdAt) + " Uhr");
    }

    protected static void appendUserFooter(EmbedBuilder eb, String userId, String avatarUrl) {
        eb.setFooter("User-ID: " + userId);
        if (avatarUrl != null) {
            eb.setThumbnail(avatarUrl);
        }
    }

    protected static void timestampNow(EmbedBuilder eb) {
        eb.setTimestamp(Instant.now());
    }
}
