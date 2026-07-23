package de.destenylp.xBotenyy.twitchbot.chat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.destenylp.xBotenyy.common.core.AbstractHttpApiClient;
import de.destenylp.xBotenyy.common.twitch.TwitchAppAccessTokenManager;
import de.destenylp.xBotenyy.common.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

public final class TwitchChatClient extends AbstractHttpApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(TwitchChatClient.class);
    private static final URI EVENTSUB_WEBSOCKET = URI.create("wss://eventsub.wss.twitch.tv/ws");
    private static final String HELIX_MESSAGES_URL = "https://api.twitch.tv/helix/chat/messages";
    private static final String HELIX_EVENTSUB_URL = "https://api.twitch.tv/helix/eventsub/subscriptions";
    private static final long DEFAULT_KEEPALIVE_TIMEOUT_SECONDS = 10;

    private final String clientId;
    private final TwitchAppAccessTokenManager appAccessTokenManager;
    private final java.util.function.Supplier<String> userAccessTokenSupplier;
    private final String botUserId;
    private final Function<String, Optional<String>> broadcasterIdResolver;
    private final Set<String> channels;
    private final long reconnectDelaySeconds;
    private final long maxReconnectDelaySeconds;

    private final HttpClient wsHttpClient = HttpClient.newHttpClient();
    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(2, runnable -> {
        Thread thread = new Thread(runnable, "twitch-chat-scheduler");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean closing = new AtomicBoolean(false);
    private final AtomicBoolean reconnectPending = new AtomicBoolean(false);
    private final Map<String, String> broadcasterIdCache = new ConcurrentHashMap<>();

    private volatile WebSocket webSocket;
    private volatile String sessionId;
    private volatile long currentReconnectDelaySeconds;
    private volatile Instant lastInboundAt = Instant.now();
    private volatile ScheduledFuture<?> keepaliveWatchdogFuture;

    private Consumer<TwitchChatMessage> onMessage = message -> {
    };
    private Runnable onConnected = () -> {
    };

    public TwitchChatClient(String clientId, TwitchAppAccessTokenManager appAccessTokenManager,
                            java.util.function.Supplier<String> userAccessTokenSupplier, String botUserId,
                            Function<String, Optional<String>> broadcasterIdResolver, Set<String> channels,
                            long reconnectDelaySeconds, long maxReconnectDelaySeconds,
                            java.time.Duration requestTimeout, int maxAttempts, java.time.Duration baseRetryDelay) {
        super(requestTimeout, maxAttempts, baseRetryDelay);
        this.clientId = clientId;
        this.appAccessTokenManager = appAccessTokenManager;
        this.userAccessTokenSupplier = userAccessTokenSupplier;
        this.botUserId = botUserId;
        this.broadcasterIdResolver = broadcasterIdResolver;
        this.channels = channels;
        this.reconnectDelaySeconds = Math.max(reconnectDelaySeconds, 1);
        this.maxReconnectDelaySeconds = Math.max(maxReconnectDelaySeconds, this.reconnectDelaySeconds);
        this.currentReconnectDelaySeconds = this.reconnectDelaySeconds;
    }

    public void onMessage(Consumer<TwitchChatMessage> listener) {
        this.onMessage = listener;
    }

    public void onConnected(Runnable listener) {
        this.onConnected = listener;
    }

    public void connect() {
        closing.set(false);
        openWebSocket(EVENTSUB_WEBSOCKET, null);
    }

    public void close() {
        closing.set(true);
        ScheduledFuture<?> watchdog = keepaliveWatchdogFuture;
        if (watchdog != null) {
            watchdog.cancel(false);
        }
        scheduler.shutdownNow();
        WebSocket socket = webSocket;
        if (socket != null) {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown");
        }
    }

    public void sendMessage(String channelLogin, String message) {
        String normalizedChannel = channelLogin.toLowerCase(Locale.ROOT);
        Optional<String> broadcasterId = resolveBroadcasterId(normalizedChannel);
        if (broadcasterId.isEmpty()) {
            LOGGER.warn("Konnte Broadcaster-ID fuer #{} nicht aufloesen, Nachricht wird nicht gesendet.", normalizedChannel);
            return;
        }
        String token = appAccessTokenManager.getAccessToken();
        if (token == null) {
            LOGGER.warn("Kein Twitch App-Access-Token verfuegbar, Nachricht wird nicht gesendet: {}", message);
            return;
        }

        String trimmed = message.length() > 500 ? message.substring(0, 500) : message;
        try {
            JsonObject body = new JsonObject();
            body.addProperty("broadcaster_id", broadcasterId.get());
            body.addProperty("sender_id", botUserId);
            body.addProperty("message", trimmed);

            HttpRequest request = requestBuilder(URI.create(HELIX_MESSAGES_URL))
                    .header("Client-Id", clientId)
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = sendWithRetry(request, HttpResponse.BodyHandlers.ofString(),
                    LOGGER, "Twitch Chat-Nachricht an #" + normalizedChannel);
            if (response.statusCode() != 200) {
                LOGGER.warn("Konnte Nachricht nicht an #{} senden (Status {}): {}",
                        normalizedChannel, response.statusCode(), response.body());
                return;
            }
            logDropReasonIfAny(normalizedChannel, response.body());
        } catch (Exception e) {
            LOGGER.warn("Fehler beim Senden der Twitch-Nachricht an #{}: {}", normalizedChannel, e.getMessage());
        }
    }

    private void logDropReasonIfAny(String channelLogin, String responseBody) {
        JsonArray data = JsonParser.parseString(responseBody).getAsJsonObject().getAsJsonArray("data");
        if (data == null || data.isEmpty()) {
            return;
        }
        JsonObject result = data.get(0).getAsJsonObject();
        if (result.has("is_sent") && !result.get("is_sent").getAsBoolean()) {
            JsonObject dropReason = result.getAsJsonObject("drop_reason");
            LOGGER.warn("Twitch hat die Nachricht an #{} verworfen: {}", channelLogin,
                    dropReason != null ? JsonUtil.optString(dropReason, "message", "unbekannt") : "unbekannt");
        }
    }

    private Optional<String> resolveBroadcasterId(String channelLogin) {
        String cached = broadcasterIdCache.get(channelLogin);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<String> resolved = broadcasterIdResolver.apply(channelLogin);
        resolved.ifPresent(id -> broadcasterIdCache.put(channelLogin, id));
        return resolved;
    }

    private void openWebSocket(URI uri, WebSocket previousSocket) {
        LOGGER.info("Verbinde mit Twitch-EventSub ...");
        wsHttpClient.newWebSocketBuilder().buildAsync(uri, new EventSubListener(previousSocket))
                .whenComplete((socket, error) -> {
                    if (error != null) {
                        LOGGER.warn("Verbindung zu Twitch-EventSub fehlgeschlagen: {}", error.getMessage());
                        scheduleReconnect();
                    }
                });
    }

    private void scheduleReconnect() {
        if (closing.get()) {
            return;
        }
        if (!reconnectPending.compareAndSet(false, true)) {
            return;
        }
        long delay = currentReconnectDelaySeconds;
        currentReconnectDelaySeconds = Math.min(currentReconnectDelaySeconds * 2, maxReconnectDelaySeconds);
        sessionId = null;
        LOGGER.info("Versuche in {}s erneut, mit Twitch-EventSub zu verbinden ...", delay);
        scheduler.schedule(() -> {
            reconnectPending.set(false);
            openWebSocket(EVENTSUB_WEBSOCKET, null);
        }, delay, TimeUnit.SECONDS);
    }

    private void resetKeepaliveWatchdog(long keepaliveTimeoutSeconds) {
        ScheduledFuture<?> previous = keepaliveWatchdogFuture;
        if (previous != null) {
            previous.cancel(false);
        }
        long intervalSeconds = Math.max(keepaliveTimeoutSeconds, 5);
        keepaliveWatchdogFuture = scheduler.scheduleAtFixedRate(() -> {
            if (closing.get()) {
                return;
            }
            Instant deadline = lastInboundAt.plusSeconds(intervalSeconds + 5);
            if (Instant.now().isAfter(deadline)) {
                LOGGER.warn("Keine Twitch-EventSub-Nachrichten seit {}s empfangen, erzwinge Reconnect.", intervalSeconds + 5);
                WebSocket socket = webSocket;
                if (socket != null) {
                    socket.sendClose(WebSocket.NORMAL_CLOSURE, "keepalive-timeout");
                } else {
                    scheduleReconnect();
                }
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    private void handleMessage(WebSocket socket, String raw, WebSocket previousSocket) {
        JsonObject root;
        try {
            root = JsonParser.parseString(raw).getAsJsonObject();
        } catch (Exception e) {
            LOGGER.warn("Konnte Twitch-EventSub-Nachricht nicht parsen: {}", e.getMessage());
            return;
        }
        JsonObject metadata = root.getAsJsonObject("metadata");
        JsonObject payload = root.getAsJsonObject("payload");
        String messageType = metadata != null ? JsonUtil.optString(metadata, "message_type") : null;
        if (messageType == null) {
            return;
        }

        switch (messageType) {
            case "session_welcome" -> handleWelcome(socket, payload, previousSocket);
            case "session_keepalive" -> lastInboundAt = Instant.now();
            case "notification" -> {
                lastInboundAt = Instant.now();
                handleNotification(payload);
            }
            case "session_reconnect" -> handleReconnect(socket, payload);
            case "revocation" -> handleRevocation(payload);
            default -> LOGGER.debug("Unbehandelte Twitch-EventSub-Nachricht: {}", messageType);
        }
    }

    private void handleWelcome(WebSocket socket, JsonObject payload, WebSocket previousSocket) {
        JsonObject session = payload != null ? payload.getAsJsonObject("session") : null;
        if (session == null) {
            LOGGER.warn("Twitch-EventSub-Willkommensnachricht ohne Session-Daten erhalten.");
            return;
        }
        String newSessionId = JsonUtil.optString(session, "id");
        long keepaliveTimeout = session.has("keepalive_timeout_seconds") && !session.get("keepalive_timeout_seconds").isJsonNull()
                ? session.get("keepalive_timeout_seconds").getAsLong()
                : DEFAULT_KEEPALIVE_TIMEOUT_SECONDS;

        lastInboundAt = Instant.now();
        resetKeepaliveWatchdog(keepaliveTimeout);
        currentReconnectDelaySeconds = reconnectDelaySeconds;
        this.webSocket = socket;
        this.sessionId = newSessionId;

        if (previousSocket != null) {
            LOGGER.info("Twitch-EventSub-Reconnect abgeschlossen.");
            previousSocket.sendClose(WebSocket.NORMAL_CLOSURE, "reconnect");
            return;
        }

        LOGGER.info("Twitch-EventSub-Session aufgebaut, richte Chat-Abos ein ...");
        for (String channel : channels) {
            scheduler.execute(() -> subscribeToChannel(channel));
        }
        onConnected.run();
    }

    private void handleReconnect(WebSocket socket, JsonObject payload) {
        JsonObject session = payload != null ? payload.getAsJsonObject("session") : null;
        String reconnectUrl = session != null ? JsonUtil.optString(session, "reconnect_url") : null;
        if (reconnectUrl == null) {
            LOGGER.warn("Twitch forderte Reconnect ohne reconnect_url an, baue neue Verbindung auf.");
            openWebSocket(EVENTSUB_WEBSOCKET, socket);
            return;
        }
        LOGGER.info("Twitch fordert Reconnect an, baue neue Verbindung auf.");
        openWebSocket(URI.create(reconnectUrl), socket);
    }

    private void handleRevocation(JsonObject payload) {
        JsonObject subscription = payload != null ? payload.getAsJsonObject("subscription") : null;
        String status = subscription != null ? JsonUtil.optString(subscription, "status", "unbekannt") : "unbekannt";
        LOGGER.warn("Twitch hat ein EventSub-Chat-Abo widerrufen (Status: {}).", status);
    }

    private void handleNotification(JsonObject payload) {
        if (payload == null) {
            return;
        }
        JsonObject subscription = payload.getAsJsonObject("subscription");
        String type = subscription != null ? JsonUtil.optString(subscription, "type") : null;
        if (!"channel.chat.message".equals(type)) {
            return;
        }
        JsonObject event = payload.getAsJsonObject("event");
        if (event == null) {
            return;
        }

        String chatterUserId = JsonUtil.optString(event, "chatter_user_id");
        if (botUserId.equals(chatterUserId)) {
            return;
        }

        String channelLogin = JsonUtil.optString(event, "broadcaster_user_login");
        String messageId = JsonUtil.optString(event, "message_id");
        String userLogin = JsonUtil.optString(event, "chatter_user_login");
        String displayName = JsonUtil.optString(event, "chatter_user_name", userLogin);

        JsonObject messageObject = event.getAsJsonObject("message");
        String content = messageObject != null ? JsonUtil.optString(messageObject, "text", "") : "";

        boolean broadcasterFlag = false;
        boolean moderatorFlag = false;
        boolean subscriberFlag = false;
        boolean vipFlag = false;
        JsonArray badges = event.getAsJsonArray("badges");
        if (badges != null) {
            for (JsonElement element : badges) {
                String setId = JsonUtil.optString(element.getAsJsonObject(), "set_id", "");
                switch (setId) {
                    case "broadcaster" -> broadcasterFlag = true;
                    case "moderator" -> moderatorFlag = true;
                    case "subscriber", "founder" -> subscriberFlag = true;
                    case "vip" -> vipFlag = true;
                    default -> {
                    }
                }
            }
        }

        TwitchChatMessage message = new TwitchChatMessage(channelLogin, messageId, chatterUserId, userLogin,
                displayName, content, moderatorFlag || broadcasterFlag, broadcasterFlag, subscriberFlag, vipFlag);
        onMessage.accept(message);
    }

    private void subscribeToChannel(String channelLogin) {
        Optional<String> broadcasterId = resolveBroadcasterId(channelLogin);
        if (broadcasterId.isEmpty()) {
            LOGGER.warn("Konnte Broadcaster-ID fuer Kanal {} nicht aufloesen, Chat-Abo wird uebersprungen.", channelLogin);
            return;
        }
        createEventSubSubscription(broadcasterId.get(), channelLogin);
    }

    private void createEventSubSubscription(String broadcasterId, String channelLogin) {
        String currentSessionId = sessionId;
        if (currentSessionId == null) {
            LOGGER.warn("Keine aktive Twitch-EventSub-Session, Chat-Abo fuer #{} wird uebersprungen.", channelLogin);
            return;
        }
        String token = userAccessTokenSupplier.get();
        if (token == null || token.isBlank()) {
            LOGGER.warn("Kein Twitch User-Access-Token verfuegbar, Chat-Abo fuer #{} wird uebersprungen.", channelLogin);
            return;
        }

        try {
            JsonObject condition = new JsonObject();
            condition.addProperty("broadcaster_user_id", broadcasterId);
            condition.addProperty("user_id", botUserId);

            JsonObject transport = new JsonObject();
            transport.addProperty("method", "websocket");
            transport.addProperty("session_id", currentSessionId);

            JsonObject body = new JsonObject();
            body.addProperty("type", "channel.chat.message");
            body.addProperty("version", "1");
            body.add("condition", condition);
            body.add("transport", transport);

            HttpRequest request = requestBuilder(URI.create(HELIX_EVENTSUB_URL))
                    .header("Client-Id", clientId)
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = sendWithRetry(request, HttpResponse.BodyHandlers.ofString(),
                    LOGGER, "Twitch EventSub-Abo fuer #" + channelLogin);
            if (response.statusCode() != 202) {
                LOGGER.warn("Konnte Twitch-Chat-Abo fuer #{} nicht anlegen (Status {}): {}",
                        channelLogin, response.statusCode(), response.body());
                return;
            }
            LOGGER.info("Twitch-Chat-Abo fuer #{} eingerichtet.", channelLogin);
        } catch (Exception e) {
            LOGGER.warn("Fehler beim Anlegen des Twitch-Chat-Abos fuer #{}: {}", channelLogin, e.getMessage());
        }
    }

    private final class EventSubListener implements WebSocket.Listener {
        private final StringBuilder buffer = new StringBuilder();
        private final WebSocket previousSocket;

        private EventSubListener(WebSocket previousSocket) {
            this.previousSocket = previousSocket;
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            webSocket.request(1);
            if (!last) {
                return CompletableFuture.completedFuture(null);
            }
            String raw = buffer.toString();
            buffer.setLength(0);
            handleMessage(webSocket, raw, previousSocket);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            LOGGER.warn("Twitch-EventSub-Verbindung geschlossen ({}): {}", statusCode, reason);
            if (TwitchChatClient.this.webSocket == webSocket || TwitchChatClient.this.webSocket == null) {
                scheduleReconnect();
            }
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            LOGGER.warn("Fehler in der Twitch-EventSub-Verbindung: {}", error.getMessage());
            if (TwitchChatClient.this.webSocket == webSocket || TwitchChatClient.this.webSocket == null) {
                scheduleReconnect();
            }
        }
    }
}