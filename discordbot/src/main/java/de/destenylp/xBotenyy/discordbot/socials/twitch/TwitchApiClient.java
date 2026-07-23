package de.destenylp.xBotenyy.discordbot.socials.twitch;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.destenylp.xBotenyy.discordbot.socials.SocialsPollStatus;
import de.destenylp.xBotenyy.common.core.AbstractHttpApiClient;
import de.destenylp.xBotenyy.common.twitch.TwitchAppAccessTokenManager;
import de.destenylp.xBotenyy.common.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TwitchApiClient extends AbstractHttpApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(TwitchApiClient.class);
    private static final String STREAMS_URL = "https://api.twitch.tv/helix/streams";
    private static final int MAX_LOGINS_PER_REQUEST = 100;

    private final String clientId;
    private final TwitchAppAccessTokenManager tokenManager;

    public TwitchApiClient(String clientId, String clientSecret, Duration requestTimeout, Duration refreshBuffer) {
        super(requestTimeout);
        this.clientId = clientId;
        this.tokenManager = new TwitchAppAccessTokenManager(clientId, clientSecret, requestTimeout, refreshBuffer);
    }

    public TwitchApiClient(String clientId, String clientSecret, Duration requestTimeout, Duration refreshBuffer,
                           int maxAttempts, Duration baseRetryDelay) {
        super(requestTimeout, maxAttempts, baseRetryDelay);
        this.clientId = clientId;
        this.tokenManager = new TwitchAppAccessTokenManager(clientId, clientSecret, requestTimeout, refreshBuffer,
                maxAttempts, baseRetryDelay);
    }

    public Map<String, TwitchStream> fetchLiveStreams(List<String> logins) {
        Map<String, TwitchStream> result = new HashMap<>();
        if (logins.isEmpty()) {
            return result;
        }

        String token = ensureAccessToken();
        if (token == null) {
            return result;
        }

        for (int start = 0; start < logins.size(); start += MAX_LOGINS_PER_REQUEST) {
            List<String> batch = logins.subList(start, Math.min(start + MAX_LOGINS_PER_REQUEST, logins.size()));
            result.putAll(fetchBatch(batch, token));
        }
        return result;
    }

    private Map<String, TwitchStream> fetchBatch(List<String> logins, String token) {
        try {
            StringBuilder query = new StringBuilder();
            for (String login : logins) {
                if (query.length() > 0) {
                    query.append('&');
                }
                query.append("user_login=").append(URLEncoder.encode(login, StandardCharsets.UTF_8));
            }

            HttpRequest request = requestBuilder(URI.create(STREAMS_URL + "?" + query))
                    .header("Client-Id", clientId)
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();

            HttpResponse<String> response = sendWithRetry(request, HttpResponse.BodyHandlers.ofString(),
                    LOGGER, "Twitch Helix Streams-Abfrage");
            if (response.statusCode() == 401) {
                LOGGER.info("Twitch Access-Token abgelaufen, erneuere Token");
                invalidateToken();
                String refreshed = ensureAccessToken();
                if (refreshed == null) {
                    return Map.of();
                }
                return fetchBatch(logins, refreshed);
            }
            if (response.statusCode() != 200) {
                LOGGER.warn("Twitch Helix API antwortete mit Status {}", response.statusCode());
                SocialsPollStatus.recordTwitchError("Helix API HTTP " + response.statusCode());
                return Map.of();
            }

            return parseStreams(response.body());
        } catch (Exception e) {
            LOGGER.warn("Konnte Twitch Streams nicht abrufen: {}", e.getMessage());
            SocialsPollStatus.recordTwitchError(e.getMessage());
            return Map.of();
        }
    }

    private Map<String, TwitchStream> parseStreams(String json) {
        Map<String, TwitchStream> result = new HashMap<>();
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonObject()) {
            return result;
        }
        JsonArray data = root.getAsJsonObject().getAsJsonArray("data");
        if (data == null) {
            return result;
        }
        for (JsonElement element : data) {
            JsonObject stream = element.getAsJsonObject();
            String login = JsonUtil.optString(stream, "user_login");
            if (login == null) {
                continue;
            }
            String id = JsonUtil.optString(stream, "id");
            String title = JsonUtil.optString(stream, "title");
            String gameName = JsonUtil.optString(stream, "game_name");
            String url = "https://twitch.tv/" + login;
            String thumbnailUrl = resolveThumbnailUrl(JsonUtil.optString(stream, "thumbnail_url"));
            result.put(login.toLowerCase(), new TwitchStream(id, login, title != null ? title : "", gameName, url, thumbnailUrl));
        }
        return result;
    }

    private String resolveThumbnailUrl(String template) {
        if (template == null || template.isBlank()) {
            return null;
        }
        return template.replace("{width}", "1280").replace("{height}", "720");
    }

    private String ensureAccessToken() {
        String token = tokenManager.getAccessToken();
        if (token == null) {
            SocialsPollStatus.recordTwitchError("OAuth Token konnte nicht abgerufen werden");
        }
        return token;
    }

    private void invalidateToken() {
        tokenManager.invalidate();
    }
}
