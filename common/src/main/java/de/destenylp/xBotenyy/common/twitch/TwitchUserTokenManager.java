package de.destenylp.xBotenyy.common.twitch;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class TwitchUserTokenManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TwitchUserTokenManager.class);
    private static final URI TOKEN_URL = URI.create("https://id.twitch.tv/oauth2/token");
    private static final Duration REFRESH_BUFFER = Duration.ofMinutes(10);

    private final String clientId;
    private final String clientSecret;
    private final Path envFilePath;
    private final String refreshTokenEnvKey;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private volatile String accessToken;
    private volatile String refreshToken;
    private volatile Instant expiresAt = Instant.EPOCH;

    public TwitchUserTokenManager(String clientId, String clientSecret, String initialRefreshToken,
                                  Path envFilePath, String refreshTokenEnvKey) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = initialRefreshToken;
        this.envFilePath = envFilePath;
        this.refreshTokenEnvKey = refreshTokenEnvKey;
    }

    public synchronized String getAccessToken() {
        if (accessToken == null || Instant.now().isAfter(expiresAt.minus(REFRESH_BUFFER))) {
            refreshNow();
        }
        return accessToken;
    }

    public synchronized String refreshNow() {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalStateException("Kein TWITCH_BOT_REFRESH_TOKEN gesetzt - siehe README fuer "
                    + "einmaliges Setup des Refresh-Tokens.");
        }
        try {
            List<String> params = new ArrayList<>();
            params.add("client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8));
            params.add("client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8));
            params.add("grant_type=refresh_token");
            params.add("refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8));
            String body = String.join("&", params);

            HttpRequest request = HttpRequest.newBuilder(TOKEN_URL)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Twitch Token-Refresh fehlgeschlagen (Status "
                        + response.statusCode() + "): " + response.body()
                        + " - der Refresh-Token ist vermutlich ungueltig geworden und muss neu erzeugt werden.");
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            String newAccessToken = json.get("access_token").getAsString();
            String newRefreshToken = json.has("refresh_token")
                    ? json.get("refresh_token").getAsString() : refreshToken;
            long expiresIn = json.has("expires_in") ? json.get("expires_in").getAsLong() : 3600;

            this.accessToken = newAccessToken;
            this.expiresAt = Instant.now().plusSeconds(expiresIn);
            if (!newRefreshToken.equals(this.refreshToken)) {
                this.refreshToken = newRefreshToken;
                persistRefreshToken(newRefreshToken);
            }

            LOGGER.info("Twitch-Access-Token erneuert, gueltig fuer {} Sekunden.", expiresIn);
            return accessToken;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("Twitch Token-Refresh fehlgeschlagen: " + e.getMessage(), e);
        }
    }

    private void persistRefreshToken(String newRefreshToken) {
        if (envFilePath == null) {
            return;
        }
        try {
            List<String> lines = Files.exists(envFilePath)
                    ? new ArrayList<>(Files.readAllLines(envFilePath, StandardCharsets.UTF_8))
                    : new ArrayList<>();
            String prefix = refreshTokenEnvKey + "=";
            boolean replaced = false;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).startsWith(prefix)) {
                    lines.set(i, prefix + newRefreshToken);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) {
                lines.add(prefix + newRefreshToken);
            }
            Files.write(envFilePath, lines, StandardCharsets.UTF_8);
            LOGGER.debug("Neuen Twitch-Refresh-Token in {} gespeichert.", envFilePath);
        } catch (IOException e) {
            LOGGER.warn("Konnte neuen Twitch-Refresh-Token nicht in {} speichern - beim naechsten Neustart "
                    + "muss ggf. der Token in der .env manuell aktualisiert werden: {}", envFilePath, e.getMessage());
        }
    }

    public static Optional<TwitchUserTokenManager> create(String clientId, String clientSecret,
                                                          String initialRefreshToken, Path envFilePath,
                                                          String refreshTokenEnvKey) {
        if (clientId == null || clientSecret == null || initialRefreshToken == null) {
            return Optional.empty();
        }
        return Optional.of(new TwitchUserTokenManager(clientId, clientSecret, initialRefreshToken,
                envFilePath, refreshTokenEnvKey));
    }
}