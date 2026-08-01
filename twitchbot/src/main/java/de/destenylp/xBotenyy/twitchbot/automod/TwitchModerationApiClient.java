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
    private final Map<String, String> gameIdCache = new ConcurrentHashMap<>();
    private volatile java.util.function.Supplier<String> broadcasterAccessTokenSupplier;

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

    public void setBroadcasterAccessTokenSupplier(java.util.function.Supplier<String> broadcasterAccessTokenSupplier) {
        this.broadcasterAccessTokenSupplier = broadcasterAccessTokenSupplier;
    }

    public boolean hasBroadcasterAccessToken() {
        return broadcasterAccessTokenSupplier != null;
    }

    public java.util.Set<String> getSubscriberUserIds(String broadcasterId) {
        return getBroadcasterPaginatedIds(HELIX_BASE + "/subscriptions?broadcaster_id=" + broadcasterId + "&first=100",
                "user_id", "Twitch Abonnenten-Liste fuer " + broadcasterId);
    }

    public java.util.Set<String> getVipUserIds(String broadcasterId) {
        return getBroadcasterPaginatedIds(HELIX_BASE + "/channels/vips?broadcaster_id=" + broadcasterId + "&first=100",
                "user_id", "Twitch VIP-Liste fuer " + broadcasterId);
    }

    public java.util.Set<String> getModeratorUserIds(String broadcasterId) {
        return getBroadcasterPaginatedIds(HELIX_BASE + "/moderation/moderators?broadcaster_id=" + broadcasterId + "&first=100",
                "user_id", "Twitch Moderatoren-Liste fuer " + broadcasterId);
    }

    private java.util.Set<String> getBroadcasterPaginatedIds(String baseUri, String idField, String description) {
        java.util.Set<String> ids = new java.util.HashSet<>();
        if (broadcasterAccessTokenSupplier == null) {
            LOGGER.warn("No broadcaster token configured, {} cannot be queried.", description);
            return ids;
        }
        String cursor = null;
        do {
            String uri = baseUri + (cursor != null ? "&after=" + cursor : "");
            try {
                HttpRequest request = requestBuilder(URI.create(uri))
                        .header("Client-Id", clientId)
                        .header("Authorization", "Bearer " + broadcasterAccessTokenSupplier.get())
                        .GET()
                        .build();
                HttpResponse<String> response = sendWithRetry(request, HttpResponse.BodyHandlers.ofString(), LOGGER, description);
                if (response.statusCode() != 200) {
                    LOGGER.warn("Could not query {} (status {}): {}", description, response.statusCode(), response.body());
                    return ids;
                }
                JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray data = body.getAsJsonArray("data");
                for (int i = 0; i < data.size(); i++) {
                    ids.add(data.get(i).getAsJsonObject().get(idField).getAsString());
                }
                JsonObject pagination = body.getAsJsonObject("pagination");
                cursor = pagination != null && pagination.has("cursor") ? pagination.get("cursor").getAsString() : null;
            } catch (Exception e) {
                LOGGER.warn("Error querying {}: {}", description, e.getMessage());
                return ids;
            }
        } while (cursor != null && !cursor.isBlank());
        return ids;
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
                LOGGER.warn("Could not resolve Twitch user ID for {} (status {}): {}", login, response.statusCode(), response.body());
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
            LOGGER.warn("Error resolving the Twitch user ID for {}: {}", login, e.getMessage());
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
                LOGGER.warn("Could not delete Twitch message {} (status {}): {}", messageId, response.statusCode(), response.body());
                return false;
            }
            return true;
        } catch (Exception e) {
            LOGGER.warn("Error deleting the Twitch message {}: {}", messageId, e.getMessage());
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
                LOGGER.warn("Could not ban/time out Twitch user {} (status {}): {}", targetUserId, response.statusCode(), response.body());
                return false;
            }
            return true;
        } catch (Exception e) {
            LOGGER.warn("Error banning/timing out Twitch user {}: {}", targetUserId, e.getMessage());
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
                    LOGGER.warn("Could not query Twitch chatters for {} (status {}): {}",
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
                LOGGER.warn("Error querying Twitch chatters for {}: {}", broadcasterId, e.getMessage());
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
                LOGGER.warn("Could not query follow status for {} in {} (status {}): {}",
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
            LOGGER.warn("Error querying the follow status for {} in {}: {}", userId, broadcasterId, e.getMessage());
            return Optional.empty();
        }
    }

    public boolean unbanUser(String broadcasterId, String moderatorId, String targetUserId) {
        try {
            URI uri = URI.create(HELIX_BASE + "/moderation/bans?broadcaster_id=" + broadcasterId
                    + "&moderator_id=" + moderatorId + "&user_id=" + targetUserId);
            HttpRequest request = authorizedRequest(uri).DELETE().build();
            HttpResponse<String> response = sendWithRetry(request, HttpResponse.BodyHandlers.ofString(),
                    LOGGER, "Twitch Unban fuer " + targetUserId);
            if (response.statusCode() != 204) {
                LOGGER.warn("Could not unban Twitch user {} (status {}): {}", targetUserId, response.statusCode(), response.body());
                return false;
            }
            return true;
        } catch (Exception e) {
            LOGGER.warn("Error unbanning Twitch user {}: {}", targetUserId, e.getMessage());
            return false;
        }
    }

    public boolean clearChat(String broadcasterId, String moderatorId) {
        try {
            URI uri = URI.create(HELIX_BASE + "/moderation/chat?broadcaster_id=" + broadcasterId
                    + "&moderator_id=" + moderatorId);
            HttpRequest request = authorizedRequest(uri).DELETE().build();
            HttpResponse<String> response = sendWithRetry(request, HttpResponse.BodyHandlers.ofString(),
                    LOGGER, "Twitch Chat leeren fuer " + broadcasterId);
            if (response.statusCode() != 204) {
                LOGGER.warn("Could not clear Twitch chat for {} (status {}): {}", broadcasterId, response.statusCode(), response.body());
                return false;
            }
            return true;
        } catch (Exception e) {
            LOGGER.warn("Error clearing the Twitch chat for {}: {}", broadcasterId, e.getMessage());
            return false;
        }
    }

    public Optional<ChannelInfo> getChannelInformation(String broadcasterId) {
        try {
            URI uri = URI.create(HELIX_BASE + "/channels?broadcaster_id=" + broadcasterId);
            HttpRequest request = authorizedRequest(uri).GET().build();
            HttpResponse<String> response = sendWithRetry(request, HttpResponse.BodyHandlers.ofString(),
                    LOGGER, "Twitch Kanalinfo fuer " + broadcasterId);
            if (response.statusCode() != 200) {
                LOGGER.warn("Could not query Twitch channel info for {} (status {}): {}",
                        broadcasterId, response.statusCode(), response.body());
                return Optional.empty();
            }
            JsonArray data = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("data");
            if (data.isEmpty()) {
                return Optional.empty();
            }
            JsonObject entry = data.get(0).getAsJsonObject();
            return Optional.of(new ChannelInfo(broadcasterId, entry.get("title").getAsString(),
                    entry.get("game_id").getAsString(), entry.get("game_name").getAsString()));
        } catch (Exception e) {
            LOGGER.warn("Error querying the Twitch channel info for {}: {}", broadcasterId, e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<String> resolveGameId(String gameName) {
        String cached = gameIdCache.get(gameName.toLowerCase(java.util.Locale.ROOT));
        if (cached != null) {
            return Optional.of(cached);
        }
        try {
            String encoded = java.net.URLEncoder.encode(gameName, StandardCharsets.UTF_8);
            URI uri = URI.create(HELIX_BASE + "/games?name=" + encoded);
            HttpRequest request = authorizedRequest(uri).GET().build();
            HttpResponse<String> response = sendWithRetry(request, HttpResponse.BodyHandlers.ofString(),
                    LOGGER, "Twitch Spiel-ID Abfrage fuer " + gameName);
            if (response.statusCode() != 200) {
                LOGGER.warn("Could not resolve Twitch game ID for {} (status {}): {}",
                        gameName, response.statusCode(), response.body());
                return Optional.empty();
            }
            JsonArray data = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("data");
            if (data.isEmpty()) {
                return Optional.empty();
            }
            String id = data.get(0).getAsJsonObject().get("id").getAsString();
            gameIdCache.put(gameName.toLowerCase(java.util.Locale.ROOT), id);
            return Optional.of(id);
        } catch (Exception e) {
            LOGGER.warn("Error resolving the Twitch game ID for {}: {}", gameName, e.getMessage());
            return Optional.empty();
        }
    }

    public boolean updateChannelInformation(String broadcasterId, String title, String gameId) {
        if (broadcasterAccessTokenSupplier == null) {
            LOGGER.warn("No broadcaster token configured, channel info for {} cannot be changed.", broadcasterId);
            return false;
        }
        try {
            JsonObject body = new JsonObject();
            if (title != null) {
                body.addProperty("title", title);
            }
            if (gameId != null) {
                body.addProperty("game_id", gameId);
            }
            URI uri = URI.create(HELIX_BASE + "/channels?broadcaster_id=" + broadcasterId);
            HttpRequest request = requestBuilder(uri)
                    .header("Client-Id", clientId)
                    .header("Authorization", "Bearer " + broadcasterAccessTokenSupplier.get())
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = sendWithRetry(request, HttpResponse.BodyHandlers.ofString(),
                    LOGGER, "Twitch Kanalinfo aendern fuer " + broadcasterId);
            if (response.statusCode() != 204) {
                LOGGER.warn("Could not change Twitch channel info for {} (status {}): {}",
                        broadcasterId, response.statusCode(), response.body());
                return false;
            }
            return true;
        } catch (Exception e) {
            LOGGER.warn("Error changing the Twitch channel info for {}: {}", broadcasterId, e.getMessage());
            return false;
        }
    }

    public boolean sendShoutout(String fromBroadcasterId, String toBroadcasterId, String moderatorId) {
        try {
            URI uri = URI.create(HELIX_BASE + "/chat/shoutouts?from_broadcaster_id=" + fromBroadcasterId
                    + "&to_broadcaster_id=" + toBroadcasterId + "&moderator_id=" + moderatorId);
            HttpRequest request = authorizedRequest(uri).POST(HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> response = sendWithRetry(request, HttpResponse.BodyHandlers.ofString(),
                    LOGGER, "Twitch Shoutout fuer " + toBroadcasterId);
            if (response.statusCode() != 204) {
                LOGGER.warn("Could not send Twitch shoutout for {} (status {}): {}",
                        toBroadcasterId, response.statusCode(), response.body());
                return false;
            }
            return true;
        } catch (Exception e) {
            LOGGER.warn("Error sending the Twitch shoutout for {}: {}", toBroadcasterId, e.getMessage());
            return false;
        }
    }

    public Optional<String> createClip(String broadcasterId) {
        try {
            URI uri = URI.create(HELIX_BASE + "/clips?broadcaster_id=" + broadcasterId);
            HttpRequest request = authorizedRequest(uri).POST(HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> response = sendWithRetry(request, HttpResponse.BodyHandlers.ofString(),
                    LOGGER, "Twitch Clip erstellen fuer " + broadcasterId);
            if (response.statusCode() != 202) {
                LOGGER.warn("Could not create Twitch clip for {} (status {}): {}",
                        broadcasterId, response.statusCode(), response.body());
                return Optional.empty();
            }
            JsonArray data = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("data");
            if (data.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(data.get(0).getAsJsonObject().get("id").getAsString());
        } catch (Exception e) {
            LOGGER.warn("Error creating the Twitch clip for {}: {}", broadcasterId, e.getMessage());
            return Optional.empty();
        }
    }

    private HttpRequest.Builder authorizedRequest(URI uri) {
        return requestBuilder(uri)
                .header("Client-Id", clientId)
                .header("Authorization", "Bearer " + moderatorAccessTokenSupplier.get());
    }

    public record ChannelInfo(String broadcasterId, String title, String gameId, String gameName) {
    }

    public record ChatterRecord(String userId, String userLogin) {
    }
}