package de.destenylp.xBotenyy.common.automod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AutomodStrikeTracker {
    private final Map<String, StrikeEntry> strikes = new ConcurrentHashMap<>();

    public int recordStrike(String key, long nowMillis, long expiryMillis) {
        StrikeEntry entry = strikes.computeIfAbsent(key, ignored -> new StrikeEntry());
        synchronized (entry) {
            if (nowMillis - entry.lastStrikeAt > expiryMillis) {
                entry.count = 0;
            }
            entry.count++;
            entry.lastStrikeAt = nowMillis;
            return entry.count;
        }
    }

    public int getCurrentStrikes(String key) {
        StrikeEntry entry = strikes.get(key);
        return entry == null ? 0 : entry.count;
    }

    public void reset(String key) {
        strikes.remove(key);
    }

    public int purgeExpired(long expiryMillis) {
        long now = System.currentTimeMillis();
        int removed = 0;
        for (Map.Entry<String, StrikeEntry> entry : strikes.entrySet()) {
            if (now - entry.getValue().lastStrikeAt > expiryMillis) {
                strikes.remove(entry.getKey());
                removed++;
            }
        }
        return removed;
    }

    private static final class StrikeEntry {
        private volatile int count = 0;
        private volatile long lastStrikeAt = 0;
    }
}
