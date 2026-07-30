package de.destenylp.xBotenyy.common.moderation.bridge;

import com.google.gson.JsonParser;
import de.destenylp.xBotenyy.common.core.AbstractHttpApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

public final class ModerationBridgeClient extends AbstractHttpApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModerationBridgeClient.class);

    public ModerationBridgeClient() {
        super(Duration.ofSeconds(10), 2, Duration.ofSeconds(2));
    }

    public Optional<BridgeActionResult> sendAction(String peerBaseUrl, String token, BridgeActionRequest request) {
        return post(peerBaseUrl + "/bridge/v1/action", token, request.toJson().toString())
                .map(body -> BridgeActionResult.fromJson(JsonParser.parseString(body).getAsJsonObject()));
    }

    public Optional<BridgeLinkConfirmResult> sendLinkConfirm(String peerBaseUrl, String token,
                                                             BridgeLinkConfirmRequest request) {
        return post(peerBaseUrl + "/bridge/v1/link/confirm", token, request.toJson().toString())
                .map(body -> BridgeLinkConfirmResult.fromJson(JsonParser.parseString(body).getAsJsonObject()));
    }

    public Optional<BridgeRoleSyncResult> sendRoleSync(String peerBaseUrl, String token, BridgeRoleSyncRequest request) {
        return post(peerBaseUrl + "/bridge/v1/roles/sync", token, request.toJson().toString())
                .map(body -> BridgeRoleSyncResult.fromJson(JsonParser.parseString(body).getAsJsonObject()));
    }

    private Optional<String> post(String url, String token, String jsonBody) {
        try {
            HttpRequest request = requestBuilder(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = sendWithRetry(request, HttpResponse.BodyHandlers.ofString(),
                    LOGGER, "Moderation-Bridge-Aufruf");
            if (response.statusCode() >= 300) {
                LOGGER.warn("Moderation-Bridge antwortete mit Status {}: {}", response.statusCode(), response.body());
                return Optional.empty();
            }
            return Optional.of(response.body());
        } catch (Exception e) {
            LOGGER.warn("Moderation-Bridge unter {} nicht erreichbar: {}", url, e.getMessage());
            return Optional.empty();
        }
    }
}
