package de.destenylp.xBotenyy.discordbot.eventlog;

import java.util.LinkedHashMap;
import java.util.Map;

public class RecentMessageCache {
    private final int maxSize;
    private final Map<Long, CachedMessage> cache;

    public RecentMessageCache(int maxSize) {
        this.maxSize = maxSize;
        this.cache = new LinkedHashMap<>(256, 0.75f, false) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, CachedMessage> eldest) {
                return size() > RecentMessageCache.this.maxSize;
            }
        };
    }

    public synchronized void put(long messageId, CachedMessage message) {
        cache.put(messageId, message);
    }

    public synchronized CachedMessage remove(long messageId) {
        return cache.remove(messageId);
    }

    public record CachedMessage(long authorId, String authorTag, String authorAvatarUrl, String content) {
    }
}
