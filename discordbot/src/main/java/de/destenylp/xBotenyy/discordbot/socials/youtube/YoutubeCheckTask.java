package de.destenylp.xBotenyy.discordbot.socials.youtube;

import de.destenylp.xBotenyy.discordbot.messaging.RenderedMessage;
import de.destenylp.xBotenyy.discordbot.observability.BotMetrics;
import de.destenylp.xBotenyy.discordbot.socials.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class YoutubeCheckTask extends AbstractFeedCheckTask<YoutubeVideo> {
    private static final Logger LOGGER = LoggerFactory.getLogger(YoutubeCheckTask.class);
    private final YoutubeFeedClient feedClient;

    public YoutubeCheckTask(JDA jda, SocialService service, YoutubeFeedClient feedClient) {
        super(jda, service);
        this.feedClient = feedClient;
    }

    @Override
    protected Logger logger() {
        return LOGGER;
    }

    @Override
    protected String platformName() {
        return "YouTube";
    }

    @Override
    protected Map<String, List<SocialAccount>> findAccounts() {
        return service.findAccountsWithYoutube();
    }

    @Override
    protected String sourceIdOf(SocialAccount account) {
        return account.getYoutubeChannelId();
    }

    @Override
    protected String describeSource(String sourceId) {
        return "Kanal " + sourceId;
    }

    @Override
    protected Optional<YoutubeVideo> fetchLatest(String sourceId) {
        return feedClient.fetchLatestVideo(sourceId);
    }

    @Override
    protected String idOf(YoutubeVideo item) {
        return item.videoId();
    }

    @Override
    protected String lastIdOf(SocialAccount account) {
        return account.getLastYoutubeVideoId();
    }

    @Override
    protected void setLastId(SocialAccount account, String id) {
        account.setLastYoutubeVideoId(id);
    }

    @Override
    protected RenderedMessage buildMessage(SocialAccount account, YoutubeVideo item, Guild guild) {
        return SocialMessageFactory.buildYoutubeMessage(account, item, guild);
    }

    @Override
    protected void recordPollAttempt() {
        SocialsPollStatus.recordYoutubePollAttempt();
    }

    @Override
    protected void recordError(String message) {
        SocialsPollStatus.recordYoutubeError(message);
    }

    @Override
    protected void onAnnounced() {
        BotMetrics.incrementYoutubeVideosAnnounced();
    }
}

