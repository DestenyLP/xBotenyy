package de.destenylp.xBotenyy.common.moderation.bridge;

import com.google.gson.JsonParser;
import de.destenylp.xBotenyy.common.core.AbstractHttpApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

public final class ModerationBridgeClient extends AbstractHttpApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModerationBridgeClient.class);
    private volatile String cachedTlsFingerprint;
    private volatile HttpClient cachedTlsClient;

    public ModerationBridgeClient() {
        super(Duration.ofSeconds(10), 2, Duration.ofSeconds(2));
    }

    public Optional<BridgeActionResult> sendAction(BridgeSettings settings, BridgeActionRequest request) {
        return post(settings, "/bridge/v1/action", request.toJson().toString())
                .map(body -> BridgeActionResult.fromJson(JsonParser.parseString(body).getAsJsonObject()));
    }

    public Optional<BridgeLinkConfirmResult> sendLinkConfirm(BridgeSettings settings,
                                                             BridgeLinkConfirmRequest request) {
        return post(settings, "/bridge/v1/link/confirm", request.toJson().toString())
                .map(body -> BridgeLinkConfirmResult.fromJson(JsonParser.parseString(body).getAsJsonObject()));
    }

    public Optional<BridgeRoleSyncResult> sendRoleSync(BridgeSettings settings, BridgeRoleSyncRequest request) {
        return post(settings, "/bridge/v1/roles/sync", request.toJson().toString())
                .map(body -> BridgeRoleSyncResult.fromJson(JsonParser.parseString(body).getAsJsonObject()));
    }

    private Optional<String> post(BridgeSettings settings, String path, String jsonBody) {
        String url = settings.peerUrl() + path;
        if (settings.tlsEnabled() && !url.toLowerCase().startsWith("https://")) {
            LOGGER.warn("bridge.tls.enabled=true, but bridge.peer.url '{}' does not start with https:// - "
                    + "Request is being aborted to prevent an accidental plaintext downgrade.", settings.peerUrl());
            return Optional.empty();
        }
        try {
            HttpClient client = clientFor(settings);
            HttpRequest request = requestBuilder(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + settings.token())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = sendWithRetry(client, request, HttpResponse.BodyHandlers.ofString(),
                    LOGGER, "Moderation-Bridge-Aufruf");
            if (response.statusCode() >= 300) {
                LOGGER.warn("Moderation bridge responded with status {}: {}", response.statusCode(), response.body());
                return Optional.empty();
            }
            return Optional.of(response.body());
        } catch (Exception e) {
            LOGGER.warn("Moderation bridge at {} unreachable: {}", url, e.getMessage());
            return Optional.empty();
        }
    }

    private synchronized HttpClient clientFor(BridgeSettings settings) throws Exception {
        if (!settings.tlsEnabled()) {
            return httpClient;
        }
        String fingerprint = settings.tlsFingerprint();
        if (cachedTlsClient != null && fingerprint.equals(cachedTlsFingerprint)) {
            return cachedTlsClient;
        }
        SSLContext sslContext = BridgeTlsSupport.buildClientContext(settings);
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(requestTimeout)
                .sslContext(sslContext)
                .sslParameters(BridgeTlsSupport.hardenedParameters(sslContext))
                .build();
        cachedTlsClient = client;
        cachedTlsFingerprint = fingerprint;
        return client;
    }
}

