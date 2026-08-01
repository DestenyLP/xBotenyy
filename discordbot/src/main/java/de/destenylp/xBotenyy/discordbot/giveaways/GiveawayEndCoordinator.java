package de.destenylp.xBotenyy.discordbot.giveaways;

import de.destenylp.xBotenyy.discordbot.observability.BotMetrics;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GiveawayEndCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger(GiveawayEndCoordinator.class);

    public void announceEnd(JDA jda, Giveaway giveaway) {
        TextChannel channel = resolveChannel(jda, giveaway);
        if (channel == null) {
            return;
        }

        if (giveaway.getMessageId() != null) {
            channel.editMessageEmbedsById(giveaway.getMessageId(), GiveawayEmbedFactory.buildEndedEmbed(giveaway))
                    .setComponents(GiveawayEmbedFactory.buildEnterComponents(giveaway))
                    .queue(success -> {
                    }, failure -> LOGGER.warn("Giveaway message {} could not be updated: {}",
                            giveaway.getMessageId(), failure.getMessage()));
        }

        channel.sendMessage(GiveawayEmbedFactory.buildWinnerAnnouncementContent(giveaway))
                .queue(success -> {
                }, failure -> LOGGER.warn("Winner announcement for giveaway {} could not be posted: {}",
                        giveaway.getId(), failure.getMessage()));

        LOGGER.info("Giveaway {} in guild {} ended with {} winner(s)", giveaway.getId(), giveaway.getGuildId(), giveaway.getWinnerIds().size());
        BotMetrics.incrementGiveawaysEnded();
    }

    public void announceReroll(JDA jda, Giveaway giveaway) {
        TextChannel channel = resolveChannel(jda, giveaway);
        if (channel == null) {
            return;
        }

        if (giveaway.getMessageId() != null) {
            channel.editMessageEmbedsById(giveaway.getMessageId(), GiveawayEmbedFactory.buildEndedEmbed(giveaway))
                    .setComponents(GiveawayEmbedFactory.buildEnterComponents(giveaway))
                    .queue(success -> {
                    }, failure -> LOGGER.warn("Giveaway message {} could not be updated: {}",
                            giveaway.getMessageId(), failure.getMessage()));
        }

        channel.sendMessage("\uD83D\uDD01 Neuauslosung: " + GiveawayEmbedFactory.buildWinnerAnnouncementContent(giveaway))
                .queue(success -> {
                }, failure -> LOGGER.warn("Reroll for giveaway {} could not be posted: {}",
                        giveaway.getId(), failure.getMessage()));

        LOGGER.info("Giveaway {} in guild {} rerolled with {} winner(s)", giveaway.getId(), giveaway.getGuildId(), giveaway.getWinnerIds().size());
    }

    public void announceCancel(JDA jda, Giveaway giveaway) {
        TextChannel channel = resolveChannel(jda, giveaway);
        if (channel == null) {
            return;
        }

        if (giveaway.getMessageId() != null) {
            channel.editMessageEmbedsById(giveaway.getMessageId(), GiveawayEmbedFactory.buildCancelledEmbed(giveaway))
                    .setComponents(GiveawayEmbedFactory.buildEnterComponents(giveaway))
                    .queue(success -> {
                    }, failure -> LOGGER.warn("Giveaway message {} could not be updated: {}",
                            giveaway.getMessageId(), failure.getMessage()));
        }

        LOGGER.info("Giveaway {} in guild {} cancelled", giveaway.getId(), giveaway.getGuildId());
    }

    private TextChannel resolveChannel(JDA jda, Giveaway giveaway) {
        if (giveaway.getChannelId() == null) {
            return null;
        }
        TextChannel channel = jda.getChannelById(TextChannel.class, giveaway.getChannelId());
        if (channel == null) {
            LOGGER.warn("Giveaway channel {} for giveaway {} no longer exists", giveaway.getChannelId(), giveaway.getId());
        }
        return channel;
    }
}
