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
import java.util.concurrent.*;
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
    private final Map<String, String> broadcasterIdCache = new ConcurrentHashMap<>();
    private Consumer<TwitchChatMessage> onMessage = message -> {
    };    private final EventSubSession botSession = new EventSubSession("bot", this::subscribeBotChannels);
    private Consumer<TwitchAutomodHeldMessage> onAutomodHeld = message -> {
    };    private final EventSubSession broadcasterSession = new EventSubSession("broadcaster", this::subscribeBroadcasterChannels);
    private Consumer<TwitchAutomodUpdateMessage> onAutomodUpdate = message -> {
    };
    private Consumer<TwitchFollowEvent> onFollow = event -> {
    };
    private Consumer<TwitchSubscribeEvent> onSubscribe = event -> {
    };
    private Consumer<TwitchRaidEvent> onRaid = event -> {
    };
    private volatile java.util.function.Supplier<String> broadcasterAccessTokenSupplier;
    private volatile boolean followAlertEnabled;
    private volatile boolean subscribeAlertEnabled;
    private volatile boolean raidAlertEnabled;
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
    }

    public void onMessage(Consumer<TwitchChatMessage> listener) {
        this.onMessage = listener;
    }

    public void onAutomodHeld(Consumer<TwitchAutomodHeldMessage> listener) {
        this.onAutomodHeld = listener;
    }

    public void onAutomodUpdate(Consumer<TwitchAutomodUpdateMessage> listener) {
        this.onAutomodUpdate = listener;
    }

    public void onFollow(Consumer<TwitchFollowEvent> listener) {
        this.onFollow = listener;
    }

    public void onSubscribe(Consumer<TwitchSubscribeEvent> listener) {
        this.onSubscribe = listener;
    }

    public void onRaid(Consumer<TwitchRaidEvent> listener) {
        this.onRaid = listener;
    }

    public void setBroadcasterAccessTokenSupplier(java.util.function.Supplier<String> supplier) {
        this.broadcasterAccessTokenSupplier = supplier;
    }

    public void setAlertSubscriptions(boolean followEnabled, boolean subscribeEnabled, boolean raidEnabled) {
        this.followAlertEnabled = followEnabled;
        this.subscribeAlertEnabled = subscribeEnabled;
        this.raidAlertEnabled = raidEnabled;
    }

    public void onConnected(Runnable listener) {
        this.onConnected = listener;
    }

    public void connect() {
        closing.set(false);
        botSession.connect();
        if (needsBroadcasterSession()) {
            broadcasterSession.connect();
        } else if (followAlertEnabled || subscribeAlertEnabled) {
            LOGGER.warn("Follow/subscribe alerts are enabled but no Twitch broadcaster token is configured, "
                    + "the dedicated broadcaster EventSub session will not be started.");
        }
    }

    private boolean needsBroadcasterSession() {
        return (followAlertEnabled || subscribeAlertEnabled) && broadcasterAccessTokenSupplier != null;
    }

    public void close() {
        closing.set(true);
        scheduler.shutdownNow();
        botSession.close();
        broadcasterSession.close();
    }

    public void sendMessage(String channelLogin, String message) {
        String normalizedChannel = channelLogin.toLowerCase(Locale.ROOT);
        Optional<String> broadcasterId = resolveBroadcasterId(normalizedChannel);
        if (broadcasterId.isEmpty()) {
            LOGGER.warn("Could not resolve broadcaster ID for #{}, message will not be sent.", normalizedChannel);
            return;
        }
        String token = appAccessTokenManager.getAccessToken();
        if (token == null) {
            LOGGER.warn("No Twitch app access token available, message will not be sent: {}", message);
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
                LOGGER.warn("Could not send message to #{} (status {}): {}",
                        normalizedChannel, response.statusCode(), response.body());
                return;
            }
            logDropReasonIfAny(normalizedChannel, response.body());
        } catch (Exception e) {
            LOGGER.warn("Error sending the Twitch message to #{}: {}", normalizedChannel, e.getMessage());
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
            LOGGER.warn("Twitch dropped the message to #{}: {}", channelLogin,
                    dropReason != null ? JsonUtil.optString(dropReason, "message", "unknown") : "unknown");
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

    private void subscribeBotChannels() {
        for (String channel : channels) {
            subscribeBotChannel(channel);
        }
        onConnected.run();
    }

    private void subscribeBroadcasterChannels() {
        for (String channel : channels) {
            subscribeBroadcasterChannel(channel);
        }
    }

    private void subscribeBotChannel(String channelLogin) {
        Optional<String> broadcasterId = resolveBroadcasterId(channelLogin);
        if (broadcasterId.isEmpty()) {
            LOGGER.warn("Could not resolve broadcaster ID for channel {}, chat subscription skipped.", channelLogin);
            return;
        }
        JsonObject chatCondition = new JsonObject();
        chatCondition.addProperty("broadcaster_user_id", broadcasterId.get());
        chatCondition.addProperty("user_id", botUserId);
        createEventSubSubscription(botSession, "channel.chat.message", "1", chatCondition, channelLogin, userAccessTokenSupplier);
        JsonObject automodCondition = new JsonObject();
        automodCondition.addProperty("broadcaster_user_id", broadcasterId.get());
        automodCondition.addProperty("moderator_user_id", botUserId);
        createEventSubSubscription(botSession, "automod.message.hold", "1", automodCondition, channelLogin, userAccessTokenSupplier);
        createEventSubSubscription(botSession, "automod.message.update", "1", automodCondition, channelLogin, userAccessTokenSupplier);
        if (raidAlertEnabled) {
            JsonObject raidCondition = new JsonObject();
            raidCondition.addProperty("to_broadcaster_user_id", broadcasterId.get());
            createEventSubSubscription(botSession, "channel.raid", "1", raidCondition, channelLogin, userAccessTokenSupplier);
        }
    }

    private void subscribeBroadcasterChannel(String channelLogin) {
        Optional<String> broadcasterId = resolveBroadcasterId(channelLogin);
        if (broadcasterId.isEmpty()) {
            LOGGER.warn("Could not resolve broadcaster ID for channel {}, follow/subscribe subscription skipped.", channelLogin);
            return;
        }
        java.util.function.Supplier<String> broadcasterToken = broadcasterAccessTokenSupplier;
        if (broadcasterToken == null) {
            return;
        }
        if (followAlertEnabled) {
            JsonObject followCondition = new JsonObject();
            followCondition.addProperty("broadcaster_user_id", broadcasterId.get());
            followCondition.addProperty("moderator_user_id", broadcasterId.get());
            createEventSubSubscription(broadcasterSession, "channel.follow", "2", followCondition, channelLogin, broadcasterToken);
        }
        if (subscribeAlertEnabled) {
            JsonObject subscribeCondition = new JsonObject();
            subscribeCondition.addProperty("broadcaster_user_id", broadcasterId.get());
            createEventSubSubscription(broadcasterSession, "channel.subscribe", "1", subscribeCondition, channelLogin, broadcasterToken);
        }
    }

    private void createEventSubSubscription(EventSubSession session, String type, String version, JsonObject condition,
                                            String channelLogin, java.util.function.Supplier<String> tokenSupplier) {
        String currentSessionId = session.sessionId;
        if (currentSessionId == null) {
            LOGGER.warn("No active Twitch EventSub session ({}), {} subscription for #{} skipped.",
                    session.name, type, channelLogin);
            return;
        }
        String token = tokenSupplier.get();
        if (token == null || token.isBlank()) {
            LOGGER.warn("No Twitch user access token available, {} subscription for #{} skipped.", type, channelLogin);
            return;
        }
        try {
            JsonObject transport = new JsonObject();
            transport.addProperty("method", "websocket");
            transport.addProperty("session_id", currentSessionId);
            JsonObject body = new JsonObject();
            body.addProperty("type", type);
            body.addProperty("version", version);
            body.add("condition", condition);
            body.add("transport", transport);
            HttpRequest request = requestBuilder(URI.create(HELIX_EVENTSUB_URL))
                    .header("Client-Id", clientId)
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = sendWithRetry(request, HttpResponse.BodyHandlers.ofString(),
                    LOGGER, "Twitch EventSub-Abo (" + type + ") fuer #" + channelLogin);
            if (response.statusCode() != 202) {
                LOGGER.warn("Could not create Twitch {} subscription for #{} (status {}): {}",
                        type, channelLogin, response.statusCode(), response.body());
                return;
            }
            LOGGER.info("Twitch {} subscription for #{} set up.", type, channelLogin);
        } catch (Exception e) {
            LOGGER.warn("Error creating the Twitch {} subscription for #{}: {}", type, channelLogin, e.getMessage());
        }
    }

    private void handleNotification(JsonObject payload) {
        if (payload == null) {
            return;
        }
        JsonObject subscription = payload.getAsJsonObject("subscription");
        String type = subscription != null ? JsonUtil.optString(subscription, "type") : null;
        JsonObject event = payload.getAsJsonObject("event");
        if (type == null || event == null) {
            return;
        }
        switch (type) {
            case "channel.chat.message" -> handleChatMessageEvent(event);
            case "automod.message.hold" -> handleAutomodHoldEvent(event);
            case "automod.message.update" -> handleAutomodUpdateEvent(event);
            case "channel.follow" -> handleFollowEvent(event);
            case "channel.subscribe" -> handleSubscribeEvent(event);
            case "channel.raid" -> handleRaidEvent(event);
            default -> {
            }
        }
    }

    private void handleChatMessageEvent(JsonObject event) {
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

    private void handleAutomodHoldEvent(JsonObject event) {
        String channelLogin = JsonUtil.optString(event, "broadcaster_user_login");
        String messageId = JsonUtil.optString(event, "message_id");
        String userId = JsonUtil.optString(event, "user_id");
        String userLogin = JsonUtil.optString(event, "user_login");
        String content = JsonUtil.optString(event, "message", "");
        String category = JsonUtil.optString(event, "category", "unknown");
        String level = event.has("level") && !event.get("level").isJsonNull()
                ? String.valueOf(event.get("level").getAsInt()) : "unknown";
        onAutomodHeld.accept(new TwitchAutomodHeldMessage(channelLogin, messageId, userId, userLogin, content, category, level));
    }

    private void handleAutomodUpdateEvent(JsonObject event) {
        String channelLogin = JsonUtil.optString(event, "broadcaster_user_login");
        String messageId = JsonUtil.optString(event, "message_id");
        String userId = JsonUtil.optString(event, "user_id");
        String userLogin = JsonUtil.optString(event, "user_login");
        String status = JsonUtil.optString(event, "status", "unknown");
        String moderatorLogin = JsonUtil.optString(event, "moderator_user_login");
        onAutomodUpdate.accept(new TwitchAutomodUpdateMessage(channelLogin, messageId, userId, userLogin, status, moderatorLogin));
    }

    private void handleFollowEvent(JsonObject event) {
        String channelLogin = JsonUtil.optString(event, "broadcaster_user_login");
        String userId = JsonUtil.optString(event, "user_id");
        String userLogin = JsonUtil.optString(event, "user_login");
        String displayName = JsonUtil.optString(event, "user_name", userLogin);
        onFollow.accept(new TwitchFollowEvent(channelLogin, userId, userLogin, displayName));
    }

    private void handleSubscribeEvent(JsonObject event) {
        String channelLogin = JsonUtil.optString(event, "broadcaster_user_login");
        String userId = JsonUtil.optString(event, "user_id");
        String userLogin = JsonUtil.optString(event, "user_login");
        String displayName = JsonUtil.optString(event, "user_name", userLogin);
        String tier = JsonUtil.optString(event, "tier", "1000");
        boolean gift = event.has("is_gift") && !event.get("is_gift").isJsonNull() && event.get("is_gift").getAsBoolean();
        onSubscribe.accept(new TwitchSubscribeEvent(channelLogin, userId, userLogin, displayName, tier, gift));
    }

    private void handleRaidEvent(JsonObject event) {
        String channelLogin = JsonUtil.optString(event, "to_broadcaster_user_login");
        String fromUserId = JsonUtil.optString(event, "from_broadcaster_user_id");
        String fromUserLogin = JsonUtil.optString(event, "from_broadcaster_user_login");
        String displayName = JsonUtil.optString(event, "from_broadcaster_user_name", fromUserLogin);
        int viewers = event.has("viewers") && !event.get("viewers").isJsonNull() ? event.get("viewers").getAsInt() : 0;
        onRaid.accept(new TwitchRaidEvent(channelLogin, fromUserId, fromUserLogin, displayName, viewers));
    }

    private final class EventSubSession {
        private final String name;
        private final Runnable onFirstWelcome;
        private final AtomicBoolean reconnectPending = new AtomicBoolean(false);
        private volatile WebSocket webSocket;
        private volatile String sessionId;
        private volatile long currentReconnectDelaySeconds;
        private volatile Instant lastInboundAt = Instant.now();
        private volatile ScheduledFuture<?> keepaliveWatchdogFuture;

        private EventSubSession(String name, Runnable onFirstWelcome) {
            this.name = name;
            this.onFirstWelcome = onFirstWelcome;
            this.currentReconnectDelaySeconds = reconnectDelaySeconds;
        }

        private void connect() {
            openWebSocket(EVENTSUB_WEBSOCKET, null);
        }

        private void close() {
            ScheduledFuture<?> watchdog = keepaliveWatchdogFuture;
            if (watchdog != null) {
                watchdog.cancel(false);
            }
            WebSocket socket = webSocket;
            if (socket != null) {
                socket.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown");
            }
        }

        private void openWebSocket(URI uri, WebSocket previousSocket) {
            LOGGER.info("Connecting to Twitch EventSub ({}) ...", name);
            wsHttpClient.newWebSocketBuilder().buildAsync(uri, new EventSubListener(this, previousSocket))
                    .whenComplete((socket, error) -> {
                        if (error != null) {
                            LOGGER.warn("Connection to Twitch EventSub ({}) failed: {}", name, error.getMessage());
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
            LOGGER.info("Retrying connection to Twitch EventSub ({}) in {}s ...", name, delay);
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
                    LOGGER.warn("No Twitch EventSub ({}) messages received for {}s, forcing reconnect.",
                            name, intervalSeconds + 5);
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
                LOGGER.warn("Could not parse Twitch EventSub ({}) message: {}", name, e.getMessage());
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
                default -> LOGGER.debug("Unhandled Twitch EventSub ({}) message: {}", name, messageType);
            }
        }

        private void handleWelcome(WebSocket socket, JsonObject payload, WebSocket previousSocket) {
            JsonObject session = payload != null ? payload.getAsJsonObject("session") : null;
            if (session == null) {
                LOGGER.warn("Received Twitch EventSub ({}) welcome message without session data.", name);
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
                LOGGER.info("Twitch EventSub ({}) reconnect completed.", name);
                previousSocket.sendClose(WebSocket.NORMAL_CLOSURE, "reconnect");
                return;
            }
            LOGGER.info("Twitch EventSub ({}) session established, setting up subscriptions ...", name);
            scheduler.execute(onFirstWelcome);
        }

        private void handleReconnect(WebSocket socket, JsonObject payload) {
            JsonObject session = payload != null ? payload.getAsJsonObject("session") : null;
            String reconnectUrl = session != null ? JsonUtil.optString(session, "reconnect_url") : null;
            if (reconnectUrl == null) {
                LOGGER.warn("Twitch requested a reconnect ({}) without a reconnect_url, establishing a new connection.", name);
                openWebSocket(EVENTSUB_WEBSOCKET, socket);
                return;
            }
            LOGGER.info("Twitch is requesting a reconnect ({}), establishing a new connection.", name);
            openWebSocket(URI.create(reconnectUrl), socket);
        }

        private void handleRevocation(JsonObject payload) {
            JsonObject subscription = payload != null ? payload.getAsJsonObject("subscription") : null;
            String status = subscription != null ? JsonUtil.optString(subscription, "status", "unknown") : "unknown";
            LOGGER.warn("Twitch revoked an EventSub ({}) chat subscription (status: {}).", name, status);
        }
    }

    private final class EventSubListener implements WebSocket.Listener {
        private final StringBuilder buffer = new StringBuilder();
        private final EventSubSession session;
        private final WebSocket previousSocket;

        private EventSubListener(EventSubSession session, WebSocket previousSocket) {
            this.session = session;
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
            session.handleMessage(webSocket, raw, previousSocket);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            LOGGER.warn("Twitch EventSub ({}) connection closed ({}): {}", session.name, statusCode, reason);
            if (session.webSocket == webSocket || session.webSocket == null) {
                session.scheduleReconnect();
            }
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            LOGGER.warn("Error in the Twitch EventSub ({}) connection: {}", session.name, error.getMessage());
            if (session.webSocket == webSocket || session.webSocket == null) {
                session.scheduleReconnect();
            }
        }
    }




}

