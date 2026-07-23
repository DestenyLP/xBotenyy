package de.destenylp.xBotenyy.common.automod;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AutomodActivityTracker {
    private final Map<String, MemberActivity> activity = new ConcurrentHashMap<>();

    public int registerMessage(String key, long nowMillis, long windowMillis) {
        MemberActivity entry = activity.computeIfAbsent(key, ignored -> new MemberActivity());
        synchronized (entry) {
            entry.lastSeenAt = nowMillis;
            entry.timestamps.addLast(nowMillis);
            while (!entry.timestamps.isEmpty() && nowMillis - entry.timestamps.peekFirst() > windowMillis) {
                entry.timestamps.pollFirst();
            }
            while (entry.timestamps.size() > 50) {
                entry.timestamps.pollFirst();
            }
            return entry.timestamps.size();
        }
    }

    public int registerDuplicate(String key, String normalizedContent, long nowMillis) {
        MemberActivity entry = activity.computeIfAbsent(key, ignored -> new MemberActivity());
        synchronized (entry) {
            entry.lastSeenAt = nowMillis;
            if (normalizedContent.isBlank()) {
                entry.repeatCount = 0;
                entry.lastContent = "";
                return 0;
            }
            if (normalizedContent.equals(entry.lastContent)) {
                entry.repeatCount++;
            } else {
                entry.repeatCount = 1;
                entry.lastContent = normalizedContent;
            }
            return entry.repeatCount;
        }
    }

    public int purgeStaleEntries(long olderThanMillis) {
        long now = System.currentTimeMillis();
        int removed = 0;
        for (Map.Entry<String, MemberActivity> entry : activity.entrySet()) {
            if (now - entry.getValue().lastSeenAt > olderThanMillis) {
                activity.remove(entry.getKey());
                removed++;
            }
        }
        return removed;
    }

    private static final class MemberActivity {
        private final Deque<Long> timestamps = new ArrayDeque<>();
        private String lastContent = "";
        private int repeatCount = 0;
        private volatile long lastSeenAt = 0;
    }
}
