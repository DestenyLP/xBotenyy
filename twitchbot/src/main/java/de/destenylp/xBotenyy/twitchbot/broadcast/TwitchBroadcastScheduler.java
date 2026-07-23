package de.destenylp.xBotenyy.twitchbot.broadcast;

import de.destenylp.xBotenyy.common.observability.Metrics;
import de.destenylp.xBotenyy.twitchbot.chat.TwitchChatClient;
import de.destenylp.xBotenyy.twitchbot.eventlog.TwitchEventLogService;
import de.destenylp.xBotenyy.twitchbot.persistence.TwitchBroadcastRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class TwitchBroadcastScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TwitchBroadcastScheduler.class);

    private final TwitchBroadcastRepository repository;
    private final TwitchChatClient chatClient;
    private final TwitchEventLogService eventLogService;
    private final Set<String> channels;
    private final ConcurrentHashMap<String, AtomicLong> messagesSinceLastBroadcast = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> roundRobinCursor = new ConcurrentHashMap<>();

    public TwitchBroadcastScheduler(TwitchBroadcastRepository repository, TwitchChatClient chatClient,
                                     TwitchEventLogService eventLogService, Set<String> channels) {
        this.repository = repository;
        this.chatClient = chatClient;
        this.eventLogService = eventLogService;
        this.channels = channels;
    }

    public void recordActivity(String channelLogin) {
        messagesSinceLastBroadcast.computeIfAbsent(channelLogin, ignored -> new AtomicLong()).incrementAndGet();
    }

    public void start(ScheduledExecutorService scheduler, long checkIntervalSeconds) {
        scheduler.scheduleAtFixedRate(this::tick, checkIntervalSeconds, checkIntervalSeconds, TimeUnit.SECONDS);
        LOGGER.info("Broadcast-System gestartet, Pruefintervall {}s.", checkIntervalSeconds);
    }

    private void tick() {
        for (String channelLogin : channels) {
            try {
                checkChannel(channelLogin);
            } catch (Exception e) {
                LOGGER.warn("Fehler beim Verarbeiten der Broadcasts fuer {}: {}", channelLogin, e.getMessage());
            }
        }
    }

    private void checkChannel(String channelLogin) {
        List<TwitchBroadcastMessage> enabled = repository.listEnabled(channelLogin);
        if (enabled.isEmpty()) {
            return;
        }

        long now = Instant.now().toEpochMilli();
        long activity = messagesSinceLastBroadcast.computeIfAbsent(channelLogin, ignored -> new AtomicLong()).get();
        AtomicInteger cursor = roundRobinCursor.computeIfAbsent(channelLogin, ignored -> new AtomicInteger());
        int size = enabled.size();
        int start = cursor.get() % size;

        for (int offset = 0; offset < size; offset++) {
            int index = (start + offset) % size;
            TwitchBroadcastMessage candidate = enabled.get(index);
            if (candidate.isDueAt(now, activity)) {
                send(channelLogin, candidate);
                cursor.set((index + 1) % size);
                messagesSinceLastBroadcast.get(channelLogin).set(0);
                return;
            }
        }
    }

    private void send(String channelLogin, TwitchBroadcastMessage broadcastMessage) {
        chatClient.sendMessage(channelLogin, broadcastMessage.message());
        repository.markSent(channelLogin, broadcastMessage.id());
        eventLogService.record(channelLogin, null, "BROADCAST_SENT", "id=" + broadcastMessage.id());
        Metrics.increment("twitch.broadcasts_sent");
    }
}
