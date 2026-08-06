package de.destenylp.xBotenyy.discordbot.socials.tiktok;
import de.destenylp.xBotenyy.discordbot.messaging.RenderedMessage;
import de.destenylp.xBotenyy.discordbot.observability.BotMetrics;
import de.destenylp.xBotenyy.discordbot.socials.AbstractFeedCheckTask;
import de.destenylp.xBotenyy.discordbot.socials.SocialAccount;
import de.destenylp.xBotenyy.discordbot.socials.SocialMessageFactory;
import de.destenylp.xBotenyy.discordbot.socials.SocialService;
import de.destenylp.xBotenyy.discordbot.socials.SocialsPollStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;
import java.util.Optional;
public final class TikTokCheckTask extends AbstractFeedCheckTask<TikTokVideo> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TikTokCheckTask.class);
    private final TikTokFeedClient feedClient;
    public TikTokCheckTask(JDA jda, SocialService service, TikTokFeedClient feedClient) {
        super(jda, service);
        this.feedClient = feedClient;
    }
    @Override
    protected Logger logger() {
        return LOGGER;
    }
    @Override
    protected String platformName() {
        return "TikTok";
    }
    @Override
    protected Map<String, List<SocialAccount>> findAccounts() {
        return service.findAccountsWithTiktok();
    }
    @Override
    protected String sourceIdOf(SocialAccount account) {
        return account.getTiktokUsername();
    }
    @Override
    protected String describeSource(String sourceId) {
        return "Account " + sourceId;
    }
    @Override
    protected Optional<TikTokVideo> fetchLatest(String sourceId) {
        return feedClient.fetchLatestVideo(sourceId);
    }
    @Override
    protected String idOf(TikTokVideo item) {
        return item.videoId();
    }
    @Override
    protected String lastIdOf(SocialAccount account) {
        return account.getLastTiktokVideoId();
    }
    @Override
    protected void setLastId(SocialAccount account, String id) {
        account.setLastTiktokVideoId(id);
    }
    @Override
    protected RenderedMessage buildMessage(SocialAccount account, TikTokVideo item, Guild guild) {
        return SocialMessageFactory.buildTiktokMessage(account, item, guild);
    }
    @Override
    protected void recordPollAttempt() {
        SocialsPollStatus.recordTiktokPollAttempt();
    }
    @Override
    protected void recordError(String message) {
        SocialsPollStatus.recordTiktokError(message);
    }
    @Override
    protected void onAnnounced() {
        BotMetrics.incrementTiktokVideosAnnounced();
    }
}
