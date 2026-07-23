package de.destenylp.xBotenyy.discordbot.socials.youtube;

import de.destenylp.xBotenyy.discordbot.messaging.MessageDispatcher;
import de.destenylp.xBotenyy.discordbot.messaging.RenderedMessage;
import de.destenylp.xBotenyy.discordbot.observability.BotMetrics;
import de.destenylp.xBotenyy.discordbot.socials.SocialAccount;
import de.destenylp.xBotenyy.discordbot.socials.SocialMessageFactory;
import de.destenylp.xBotenyy.discordbot.socials.SocialService;
import de.destenylp.xBotenyy.discordbot.socials.SocialsPollStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class YoutubeCheckTask implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(YoutubeCheckTask.class);

    private final JDA jda;
    private final SocialService service;
    private final YoutubeFeedClient feedClient;

    public YoutubeCheckTask(JDA jda, SocialService service, YoutubeFeedClient feedClient) {
        this.jda = jda;
        this.service = service;
        this.feedClient = feedClient;
    }

    private record GuildAccount(String guildId, SocialAccount account) {
    }

    @Override
    public void run() {
        SocialsPollStatus.recordYoutubePollAttempt();
        try {
            checkChannels();
        } catch (Exception e) {
            SocialsPollStatus.recordYoutubeError(e.getMessage());
            LOGGER.error("Fehler bei der Ueberpruefung der YouTube Kanaele: ", e);
        }
    }

    private void checkChannels() {
        Map<String, List<SocialAccount>> byGuild = service.findAccountsWithYoutube();
        if (byGuild.isEmpty()) {
            return;
        }

        Map<String, List<GuildAccount>> byChannel = new HashMap<>();
        byGuild.forEach((guildId, accounts) -> accounts.forEach(account ->
                byChannel.computeIfAbsent(account.getYoutubeChannelId(), key -> new ArrayList<>())
                        .add(new GuildAccount(guildId, account))));

        byChannel.forEach(this::checkChannel);
    }

    private void checkChannel(String youtubeChannelId, List<GuildAccount> guildAccounts) {
        try {
            Optional<YoutubeVideo> latest = feedClient.fetchLatestVideo(youtubeChannelId);
            if (latest.isEmpty()) {
                return;
            }
            YoutubeVideo video = latest.get();
            for (GuildAccount guildAccount : guildAccounts) {
                handleAccount(guildAccount.guildId(), guildAccount.account(), video);
            }
        } catch (Exception e) {
            SocialsPollStatus.recordYoutubeError("Kanal " + youtubeChannelId + ": " + e.getMessage());
            LOGGER.warn("Fehler bei der Ueberpruefung des YouTube Kanals {}: {}", youtubeChannelId, e.getMessage());
        }
    }

    private void handleAccount(String guildId, SocialAccount account, YoutubeVideo video) {
        if (account.getLastYoutubeVideoId() == null) {
            account.setLastYoutubeVideoId(video.videoId());
            service.saveAccount(guildId, account);
            return;
        }

        if (account.getLastYoutubeVideoId().equals(video.videoId())) {
            return;
        }

        account.setLastYoutubeVideoId(video.videoId());
        service.saveAccount(guildId, account);
        announce(guildId, account, video);
    }

    private void announce(String guildId, SocialAccount account, YoutubeVideo video) {
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            return;
        }
        TextChannel channel = jda.getChannelById(TextChannel.class, account.getChannelId());
        if (channel == null) {
            LOGGER.warn("Ankuendigungskanal {} fuer Social-Account {} in Guild {} existiert nicht mehr",
                    account.getChannelId(), account.getId(), guildId);
            return;
        }

        RenderedMessage message = SocialMessageFactory.buildYoutubeMessage(account, video, guild);
        MessageDispatcher.prepare(channel, message).ifPresent(action -> action.queue(
                success -> BotMetrics.incrementYoutubeVideosAnnounced(),
                failure -> LOGGER.warn("Konnte YouTube Ankuendigung fuer Account {} nicht senden: {}",
                        account.getId(), failure.getMessage())));
        LOGGER.info("Neues YouTube Video fuer Account {} in Guild {} angekuendigt: {}", account.getId(), guildId, video.videoId());
    }
}
