package de.destenylp.xBotenyy.common.twitch;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.destenylp.xBotenyy.common.core.AbstractHttpApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class TwitchAppAccessTokenManager extends AbstractHttpApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(TwitchAppAccessTokenManager.class);
    private static final String TOKEN_URL = "https://id.twitch.tv/oauth2/token";

    private final String clientId;
    private final String clientSecret;
    private final Duration refreshBuffer;

    private volatile String accessToken;
    private volatile Instant expiresAt = Instant.EPOCH;

    public TwitchAppAccessTokenManager(String clientId, String clientSecret, Duration requestTimeout,
                                       Duration refreshBuffer) {
        super(requestTimeout);
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshBuffer = refreshBuffer;
    }

    public TwitchAppAccessTokenManager(String clientId, String clientSecret, Duration requestTimeout,
                                       Duration refreshBuffer, int maxAttempts, Duration baseRetryDelay) {
        super(requestTimeout, maxAttempts, baseRetryDelay);
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshBuffer = refreshBuffer;
    }

    public synchronized String getAccessToken() {
        if (clientId == null || clientSecret == null) {
            return null;
        }
        if (accessToken != null && Instant.now().isBefore(expiresAt.minus(refreshBuffer))) {
            return accessToken;
        }
        return requestNewToken();
    }

    public synchronized void invalidate() {
        accessToken = null;
        expiresAt = Instant.EPOCH;
    }

    private String requestNewToken() {
        try {
            List<String> params = new ArrayList<>();
            params.add("client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8));
            params.add("client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8));
            params.add("grant_type=client_credentials");
            String query = String.join("&", params);

            HttpRequest request = requestBuilder(URI.create(TOKEN_URL + "?" + query))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = sendWithRetry(request, HttpResponse.BodyHandlers.ofString(),
                    LOGGER, "Twitch App-Access-Token Anfrage");
            if (response.statusCode() != 200) {
                LOGGER.warn("Twitch App-Access-Token konnte nicht abgerufen werden, Status {}: {}",
                        response.statusCode(), response.body());
                return null;
            }

            JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
            String token = body.get("access_token").getAsString();
            long expiresIn = body.has("expires_in") ? body.get("expires_in").getAsLong() : 3600;

            accessToken = token;
            expiresAt = Instant.now().plusSeconds(expiresIn);
            return token;
        } catch (Exception e) {
            LOGGER.warn("Fehler beim Abrufen des Twitch App-Access-Tokens: {}", e.getMessage());
            return null;
        }
    }
}
