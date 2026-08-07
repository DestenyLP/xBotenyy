package de.destenylp.xBotenyy.discordbot.socials.youtube;

import de.destenylp.xBotenyy.discordbot.socials.AbstractRssFeedClient;
import de.destenylp.xBotenyy.discordbot.socials.SocialsPollStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

public class YoutubeFeedClient extends AbstractRssFeedClient<YoutubeVideo> {
    private static final Logger LOGGER = LoggerFactory.getLogger(YoutubeFeedClient.class);
    private static final String FEED_URL = "https://www.youtube.com/feeds/videos.xml?channel_id=";

    public YoutubeFeedClient(Duration requestTimeout) {
        super(requestTimeout, "entry");
    }

    public YoutubeFeedClient(Duration requestTimeout, int maxAttempts, Duration baseRetryDelay) {
        super(requestTimeout, maxAttempts, baseRetryDelay, "entry");
    }

    @Override
    protected void customizeHttpClient(HttpClient.Builder builder) {
        builder.followRedirects(HttpClient.Redirect.NORMAL);
    }

    public Optional<YoutubeVideo> fetchLatestVideo(String channelId) {
        return fetchLatestEntry(channelId);
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
        return "Kanal " + sourceId;
    }

    @Override
    protected void onError(String message) {
        SocialsPollStatus.recordYoutubeError(message);
    }

    @Override
    protected Optional<YoutubeVideo> parseEntry(Element entry) {
        String videoId = textOf(entry, "yt:videoId");
        String title = textOf(entry, "title");
        String link = attributeOf(entry, "link", "href");
        if (videoId == null || videoId.isBlank()) {
            return Optional.empty();
        }
        String url = link != null ? link : "https://www.youtube.com/watch?v=" + videoId;
        String thumbnail = resolveBestThumbnail(videoId);
        return Optional.of(new YoutubeVideo(videoId, title != null ? title : "", url, thumbnail));
    }

    private String resolveBestThumbnail(String videoId) {
        String maxres = "https://i.ytimg.com/vi/" + videoId + "/maxresdefault.jpg";
        String fallback = "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg";
        try {
            HttpRequest headRequest = requestBuilder(URI.create(maxres))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<Void> headResponse = httpClient.send(headRequest, HttpResponse.BodyHandlers.discarding());
            return headResponse.statusCode() == 200 ? maxres : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }
}

