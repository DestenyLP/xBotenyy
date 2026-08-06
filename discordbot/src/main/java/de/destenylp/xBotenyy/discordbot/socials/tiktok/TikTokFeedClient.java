package de.destenylp.xBotenyy.discordbot.socials.tiktok;
import de.destenylp.xBotenyy.discordbot.socials.AbstractRssFeedClient;
import de.destenylp.xBotenyy.discordbot.socials.SocialsPollStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class TikTokFeedClient extends AbstractRssFeedClient<TikTokVideo> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TikTokFeedClient.class);
    private static final String FEED_URL = "https://rsshub.app/tiktok/user/";
    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile("/video/(\\d+)");
    private static final Pattern IMAGE_SRC_PATTERN = Pattern.compile("<img[^>]+src=\"([^\"]+)\"");
    public TikTokFeedClient(Duration requestTimeout) {
        super(requestTimeout, "item");
    }
    public TikTokFeedClient(Duration requestTimeout, int maxAttempts, Duration baseRetryDelay) {
        super(requestTimeout, maxAttempts, baseRetryDelay, "item");
    }
    @Override
    protected void customizeHttpClient(HttpClient.Builder builder) {
        builder.followRedirects(HttpClient.Redirect.NORMAL);
    }
    public Optional<TikTokVideo> fetchLatestVideo(String username) {
        return fetchLatestEntry(username);
    }
    @Override
    protected Logger logger() {
        return LOGGER;
    }
    @Override
    protected String buildFeedUrl(String sourceId) {
        return FEED_URL + sourceId;
    }
    @Override
    protected String describeSource(String sourceId) {
        return "Account " + sourceId;
    }
    @Override
    protected void onError(String message) {
        SocialsPollStatus.recordTiktokError(message);
    }
    @Override
    protected Optional<TikTokVideo> parseEntry(Element entry) {
        String link = textOf(entry, "link");
        String title = textOf(entry, "title");
        String description = textOf(entry, "description");
        if (link == null || link.isBlank()) {
            return Optional.empty();
        }
        String videoId = extractVideoId(link);
        if (videoId == null) {
            return Optional.empty();
        }
        String thumbnail = extractThumbnail(description);
        return Optional.of(new TikTokVideo(videoId, title != null ? title : "", link, thumbnail));
    }
    private String extractVideoId(String link) {
        Matcher matcher = VIDEO_ID_PATTERN.matcher(link);
        return matcher.find() ? matcher.group(1) : null;
    }
    private String extractThumbnail(String description) {
        if (description == null) {
            return null;
        }
        Matcher matcher = IMAGE_SRC_PATTERN.matcher(description);
        return matcher.find() ? matcher.group(1) : null;
    }
}
