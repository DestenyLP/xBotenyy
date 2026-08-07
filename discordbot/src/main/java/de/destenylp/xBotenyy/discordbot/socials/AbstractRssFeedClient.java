package de.destenylp.xBotenyy.discordbot.socials;

import de.destenylp.xBotenyy.common.core.AbstractHttpApiClient;
import org.slf4j.Logger;
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
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

public abstract class AbstractRssFeedClient<T> extends AbstractHttpApiClient {
    private final String entryTagName;

    protected AbstractRssFeedClient(Duration requestTimeout, String entryTagName) {
        super(requestTimeout);
        this.entryTagName = entryTagName;
    }

    protected AbstractRssFeedClient(Duration requestTimeout, int maxAttempts, Duration baseRetryDelay,
                                    String entryTagName) {
        super(requestTimeout, maxAttempts, baseRetryDelay);
        this.entryTagName = entryTagName;
    }

    public Optional<T> fetchLatestEntry(String sourceId) {
        try {
            HttpRequest request = requestBuilder(URI.create(buildFeedUrl(sourceId)))
                    .GET()
                    .build();
            HttpResponse<String> response = sendWithRetry(request, HttpResponse.BodyHandlers.ofString(),
                    logger(), describeSource(sourceId));
            if (response.statusCode() != 200) {
                String message = describeSource(sourceId) + ": HTTP " + response.statusCode();
                logger().warn("{} responded with status {}", describeSource(sourceId), response.statusCode());
                onError(message);
                return Optional.empty();
            }
            return parseLatestEntry(response.body());
        } catch (Exception e) {
            logger().warn("Could not fetch feed for {}: {}", describeSource(sourceId), e.getMessage());
            onError(describeSource(sourceId) + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<T> parseLatestEntry(String xml) {
        try {
            Document document = newSecureDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            NodeList entries = document.getElementsByTagName(entryTagName);
            if (entries.getLength() == 0) {
                return Optional.empty();
            }
            return parseEntry((Element) entries.item(0));
        } catch (Exception e) {
            logger().warn("Could not parse feed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    protected abstract Logger logger();

    protected abstract String buildFeedUrl(String sourceId);

    protected abstract String describeSource(String sourceId);

    protected abstract void onError(String message);

    protected abstract Optional<T> parseEntry(Element entry);

    protected DocumentBuilder newSecureDocumentBuilder() throws Exception {
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

    protected String textOf(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return null;
        }
        Node node = nodes.item(0);
        return node.getTextContent();
    }

    protected String attributeOf(Element parent, String tagName, String attribute) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return null;
        }
        Node node = nodes.item(0);
        Node attr = node.getAttributes() != null ? node.getAttributes().getNamedItem(attribute) : null;
        return attr != null ? attr.getTextContent() : null;
    }
}

