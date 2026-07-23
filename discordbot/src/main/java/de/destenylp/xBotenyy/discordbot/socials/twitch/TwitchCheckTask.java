package de.destenylp.xBotenyy.discordbot.socials.twitch;

import de.destenylp.xBotenyy.discordbot.messaging.MessageDispatcher;
import de.destenylp.xBotenyy.discordbot.messaging.RenderedMessage;
import de.destenylp.xBotenyy.discordbot.observability.BotMetrics;
import de.destenylp.xBotenyy.discordbot.socials.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TwitchCheckTask implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(TwitchCheckTask.class);

    private final JDA jda;
    private final SocialService service;
    private final TwitchApiClient apiClient;
    private final AtomicBoolean missingCredentialsWarned = new AtomicBoolean(false);

    public TwitchCheckTask(JDA jda, SocialService service, TwitchApiClient apiClient) {
        this.jda = jda;
        this.service = service;
        this.apiClient = apiClient;
    }

    private record GuildAccount(String guildId, SocialAccount account) {
    }

    @Override
    public void run() {
        SocialsPollStatus.recordTwitchPollAttempt();
        try {
            checkStreams();
        } catch (Exception e) {
            SocialsPollStatus.recordTwitchError(e.getMessage());
            LOGGER.error("Fehler bei der Ueberpruefung der Twitch Streams: ", e);
        }
    }

    private void checkStreams() {
        if (apiClient == null) {
            if (missingCredentialsWarned.compareAndSet(false, true)) {
                LOGGER.warn("Twitch Anmeldedaten sind nicht konfiguriert, Twitch Ueberwachung wird uebersprungen.");
            }
            SocialsPollStatus.recordTwitchError("Twitch Anmeldedaten sind nicht konfiguriert.");
            return;
        }

        Map<String, List<SocialAccount>> byGuild = service.findAccountsWithTwitch();
        if (byGuild.isEmpty()) {
            return;
        }

        List<GuildAccount> guildAccounts = new ArrayList<>();
        List<String> logins = new ArrayList<>();
        byGuild.forEach((guildId, accounts) -> accounts.forEach(account -> {
            guildAccounts.add(new GuildAccount(guildId, account));
            logins.add(account.getTwitchLogin().toLowerCase());
        }));

        Map<String, TwitchStream> liveStreams = apiClient.fetchLiveStreams(logins.stream().distinct().toList());

        for (GuildAccount guildAccount : guildAccounts) {
            handleAccount(guildAccount.guildId(), guildAccount.account(), liveStreams);
        }
    }

    private void handleAccount(String guildId, SocialAccount account, Map<String, TwitchStream> liveStreams) {
        TwitchStream stream = liveStreams.get(account.getTwitchLogin().toLowerCase());

        if (stream == null) {
            if (account.isTwitchCurrentlyLive()) {
                account.markTwitchOffline();
                service.saveAccount(guildId, account);
            }
            return;
        }

        boolean alreadyAnnounced = account.isTwitchCurrentlyLive()
                && stream.id() != null
                && stream.id().equals(account.getLastTwitchStreamId());
        if (alreadyAnnounced) {
            return;
        }

        account.markTwitchLive(stream.id());
        service.saveAccount(guildId, account);
        announce(guildId, account, stream);
    }

    private void announce(String guildId, SocialAccount account, TwitchStream stream) {
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

        RenderedMessage message = SocialMessageFactory.buildTwitchMessage(account, stream, guild);
        MessageDispatcher.prepare(channel, message).ifPresent(action -> action.queue(
                success -> BotMetrics.incrementTwitchStreamsAnnounced(),
                failure -> LOGGER.warn("Konnte Twitch Ankuendigung fuer Account {} nicht senden: {}",
                        account.getId(), failure.getMessage())));
        LOGGER.info("Twitch Livestream fuer Account {} in Guild {} angekuendigt: {}", account.getId(), guildId, stream.id());
    }
}
