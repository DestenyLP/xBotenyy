package de.destenylp.xBotenyy.twitchbot.automod;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.destenylp.xBotenyy.common.core.AbstractHttpApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class TwitchModerationApiClient extends AbstractHttpApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(TwitchModerationApiClient.class);
    private static final String HELIX_BASE = "https://api.twitch.tv/helix";

    private final String clientId;
    private final java.util.function.Supplier<String> moderatorAccessTokenSupplier;
    private final Map<String, String> userIdCache = new ConcurrentHashMap<>();

    public TwitchModerationApiClient(String clientId, String moderatorAccessToken, Duration requestTimeout) {
        super(requestTimeout);
        this.clientId = clientId;
        this.moderatorAccessTokenSupplier = () -> moderatorAccessToken;
    }

    public TwitchModerationApiClient(String clientId, String moderatorAccessToken, Duration requestTimeout,
                                     int maxAttempts, Duration baseRetryDelay) {
        super(requestTimeout, maxAttempts, baseRetryDelay);
        this.clientId = clientId;
        this.moderatorAccessTokenSupplier = () -> moderatorAccessToken;
    }

    public TwitchModerationApiClient(String clientId, java.util.function.Supplier<String> moderatorAccessTokenSupplier,
                                     Duration requestTimeout, int maxAttempts, Duration baseRetryDelay) {
        super(requestTimeout, maxAttempts, baseRetryDelay);
        this.clientId = clientId;
        this.moderatorAccessTokenSupplier = moderatorAccessTokenSupplier;
    }

    public Optional<String> resolveUserId(String login) {
        String cached = userIdCache.get(login.toLowerCase());
        if (cached != null) {
            return Optional.of(cached);
        }
        try {
            HttpRequest request = authorizedRequest(URI.create(HELIX_BASE + "/users?login=" + login))
                    .GET()
                    .build();
            HttpResponse<String> response = sendWithRetry(request, HttpResponse.BodyHandlers.ofString(),
                    LOGGER, "Twitch Nutzer-ID Abfrage fuer " + login);
            if (response.statusCode() != 200) {
                LOGGER.warn("Konnte Twitch-Nutzer-ID fuer {} nicht aufloesen (Status {}): {}", login, response.statusCode(), response.body());
                return Optional.empty();
            }
            JsonArray data = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("data");
            if (data.isEmpty()) {
                return Optional.empty();
            }
            String id = data.get(0).getAsJsonObject().get("id").getAsString();
            userIdCache.put(login.toLowerCase(), id);
            return Optional.of(id);
        } catch (Exception e) {
            LOGGER.warn("Fehler beim Aufloesen der Twitch-Nutzer-ID fuer {}: {}", login, e.getMessage());
            return Optional.empty();
        }
    }

    public boolean deleteMessage(String broadcasterId, String moderatorId, String messageId) {
        try {
            URI uri = URI.create(HELIX_BASE + "/moderation/chat?broadcaster_id=" + broadcasterId
                    + "&moderator_id=" + moderatorId + "&message_id=" + messageId);
            HttpRequest request = authorizedRequest(uri).DELETE().build();
            HttpResponse<String> response = sendWithRetry(request, HttpResponse.BodyHandlers.ofString(),
                    LOGGER, "Twitch Nachricht loeschen (" + messageId + ")");
            if (response.statusCode() != 204) {
                LOGGER.warn("Konnte Twitch-Nachricht {} nicht loeschen (Status {}): {}", messageId, response.statusCode(), response.body());
                return false;
            }
            return true;
        } catch (Exception e) {
            LOGGER.warn("Fehler beim Loeschen der Twitch-Nachricht {}: {}", messageId, e.getMessage());
            return false;
        }
    }

    public boolean banUser(String broadcasterId, String moderatorId, String targetUserId, String reason, long durationSeconds) {
        try {
            JsonObject data = new JsonObject();
            data.addProperty("user_id", targetUserId);
            data.addProperty("reason", reason.length() > 500 ? reason.substring(0, 500) : reason);
            if (durationSeconds > 0) {
                data.addProperty("duration", durationSeconds);
            }
            JsonObject body = new JsonObject();
            body.add("data", data);

            URI uri = URI.create(HELIX_BASE + "/moderation/bans?broadcaster_id=" + broadcasterId + "&moderator_id=" + moderatorId);
            HttpRequest request = authorizedRequest(uri)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = sendWithRetry(request, HttpResponse.BodyHandlers.ofString(),
                    LOGGER, "Twitch Ban/Timeout fuer " + targetUserId);
            if (response.statusCode() != 200) {
                LOGGER.warn("Konnte Twitch-Nutzer {} nicht bannen/timeouten (Status {}): {}", targetUserId, response.statusCode(), response.body());
                return false;
            }
            return true;
        } catch (Exception e) {
            LOGGER.warn("Fehler beim Bannen/Timeouten des Twitch-Nutzers {}: {}", targetUserId, e.getMessage());
            return false;
        }
    }

    public List<ChatterRecord> getAllChatters(String broadcasterId, String moderatorId) {
        List<ChatterRecord> chatters = new ArrayList<>();
        String cursor = null;
        do {
            StringBuilder uriBuilder = new StringBuilder(HELIX_BASE)
                    .append("/chat/chatters?broadcaster_id=").append(broadcasterId)
                    .append("&moderator_id=").append(moderatorId)
                    .append("&first=1000");
            if (cursor != null) {
                uriBuilder.append("&after=").append(cursor);
            }
            try {
                HttpRequest request = authorizedRequest(URI.create(uriBuilder.toString())).GET().build();
                HttpResponse<String> response = sendWithRetry(request, HttpResponse.BodyHandlers.ofString(),
                        LOGGER, "Twitch Chatters Abfrage fuer " + broadcasterId);
                if (response.statusCode() != 200) {
                    LOGGER.warn("Konnte Twitch-Chatter fuer {} nicht abfragen (Status {}): {}",
                            broadcasterId, response.statusCode(), response.body());
                    return chatters;
                }
                JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray data = body.getAsJsonArray("data");
                for (int i = 0; i < data.size(); i++) {
                    JsonObject entry = data.get(i).getAsJsonObject();
                    chatters.add(new ChatterRecord(entry.get("user_id").getAsString(), entry.get("user_login").getAsString()));
                }
                JsonObject pagination = body.getAsJsonObject("pagination");
                cursor = pagination != null && pagination.has("cursor") ? pagination.get("cursor").getAsString() : null;
            } catch (Exception e) {
                LOGGER.warn("Fehler beim Abfragen der Twitch-Chatter fuer {}: {}", broadcasterId, e.getMessage());
                return chatters;
            }
        } while (cursor != null && !cursor.isBlank());
        return chatters;
    }

    public Optional<Instant> getFollowedAt(String broadcasterId, String userId) {
        try {
            URI uri = URI.create(HELIX_BASE + "/channels/followers?broadcaster_id=" + broadcasterId + "&user_id=" + userId);
            HttpRequest request = authorizedRequest(uri).GET().build();
            HttpResponse<String> response = sendWithRetry(request, HttpResponse.BodyHandlers.ofString(),
                    LOGGER, "Twitch Follower-Abfrage fuer " + userId + " in " + broadcasterId);
            if (response.statusCode() != 200) {
                LOGGER.warn("Konnte Follow-Status fuer {} in {} nicht abfragen (Status {}): {}",
                        userId, broadcasterId, response.statusCode(), response.body());
                return Optional.empty();
            }
            JsonArray data = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("data");
            if (data.isEmpty()) {
                return Optional.empty();
            }
            String followedAt = data.get(0).getAsJsonObject().get("followed_at").getAsString();
            return Optional.of(Instant.parse(followedAt));
        } catch (Exception e) {
            LOGGER.warn("Fehler beim Abfragen des Follow-Status fuer {} in {}: {}", userId, broadcasterId, e.getMessage());
            return Optional.empty();
        }
    }

    public record ChatterRecord(String userId, String userLogin) {
    }

    private HttpRequest.Builder authorizedRequest(URI uri) {
        return requestBuilder(uri)
                .header("Client-Id", clientId)
                .header("Authorization", "Bearer " + moderatorAccessTokenSupplier.get());
    }
}