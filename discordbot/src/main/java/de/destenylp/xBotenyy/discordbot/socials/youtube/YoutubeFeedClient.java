package de.destenylp.xBotenyy.discordbot.socials.youtube;

import de.destenylp.xBotenyy.discordbot.socials.SocialsPollStatus;
import de.destenylp.xBotenyy.common.core.AbstractHttpApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

public class YoutubeFeedClient extends AbstractHttpApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(YoutubeFeedClient.class);
    private static final String FEED_URL = "https://www.youtube.com/feeds/videos.xml?channel_id=";

    public YoutubeFeedClient(Duration requestTimeout) {
        super(requestTimeout);
    }

    public YoutubeFeedClient(Duration requestTimeout, int maxAttempts, Duration baseRetryDelay) {
        super(requestTimeout, maxAttempts, baseRetryDelay);
    }

    @Override
    protected void customizeHttpClient(HttpClient.Builder builder) {
        builder.followRedirects(HttpClient.Redirect.NORMAL);
    }

    public Optional<YoutubeVideo> fetchLatestVideo(String channelId) {
        try {
            HttpRequest request = requestBuilder(URI.create(FEED_URL + channelId))
                    .GET()
                    .build();
            HttpResponse<String> response = sendWithRetry(request, HttpResponse.BodyHandlers.ofString(),
                    LOGGER, "YouTube Feed fuer Kanal " + channelId);
            if (response.statusCode() != 200) {
                String message = "Kanal " + channelId + ": HTTP " + response.statusCode();
                LOGGER.warn("YouTube Feed fuer Kanal {} antwortete mit Status {}", channelId, response.statusCode());
                SocialsPollStatus.recordYoutubeError(message);
                return Optional.empty();
            }
            return parseLatestEntry(response.body());
        } catch (Exception e) {
            LOGGER.warn("Konnte YouTube Feed fuer Kanal {} nicht abrufen: {}", channelId, e.getMessage());
            SocialsPollStatus.recordYoutubeError("Kanal " + channelId + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<YoutubeVideo> parseLatestEntry(String xml) {
        try {
            Document document = newSecureDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            NodeList entries = document.getElementsByTagName("entry");
            if (entries.getLength() == 0) {
                return Optional.empty();
            }
            Element entry = (Element) entries.item(0);
            String videoId = textOf(entry, "yt:videoId");
            String title = textOf(entry, "title");
            String link = attributeOf(entry, "link", "href");
            if (videoId == null || videoId.isBlank()) {
                return Optional.empty();
            }
            String url = link != null ? link : "https://www.youtube.com/watch?v=" + videoId;
            String thumbnail = resolveBestThumbnail(videoId);
            return Optional.of(new YoutubeVideo(videoId, title != null ? title : "", url, thumbnail));
        } catch (Exception e) {
            LOGGER.warn("Konnte YouTube Feed nicht parsen: {}", e.getMessage());
            return Optional.empty();
        }
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

    private DocumentBuilder newSecureDocumentBuilder() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setNamespaceAware(false);
        return factory.newDocumentBuilder();
    }

    private String textOf(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return null;
        }
        Node node = nodes.item(0);
        return node.getTextContent();
    }

    private String attributeOf(Element parent, String tagName, String attribute) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return null;
        }
        Node node = nodes.item(0);
        Node attr = node.getAttributes() != null ? node.getAttributes().getNamedItem(attribute) : null;
        return attr != null ? attr.getTextContent() : null;
    }
}
