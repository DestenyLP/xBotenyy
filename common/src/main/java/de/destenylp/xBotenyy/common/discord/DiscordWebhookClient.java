package de.destenylp.xBotenyy.common.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.destenylp.xBotenyy.common.core.AbstractHttpApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DiscordWebhookClient extends AbstractHttpApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordWebhookClient.class);

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "discord-webhook-log");
        thread.setDaemon(true);
        return thread;
    });

    public DiscordWebhookClient() {
        this(Duration.ofSeconds(10), 2, Duration.ofSeconds(2));
    }

    public DiscordWebhookClient(Duration requestTimeout, int maxAttempts, Duration baseRetryDelay) {
        super(requestTimeout, maxAttempts, baseRetryDelay);
    }

    public void sendEmbedAsync(String webhookUrl, JsonObject embed) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }
        executor.submit(() -> sendEmbed(webhookUrl, embed));
    }

    private void sendEmbed(String webhookUrl, JsonObject embed) {
        try {
            JsonArray embeds = new JsonArray();
            embeds.add(embed);
            JsonObject body = new JsonObject();
            body.add("embeds", embeds);

            HttpRequest request = requestBuilder(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = sendWithRetry(request, HttpResponse.BodyHandlers.ofString(),
                    LOGGER, "Discord-Webhook-Log");
            if (response.statusCode() >= 300) {
                LOGGER.warn("Discord-Webhook lieferte Status {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            LOGGER.warn("Fehler beim Senden des Discord-Webhook-Logs: {}", e.getMessage());
        }
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
