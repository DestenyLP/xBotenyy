package de.destenylp.xBotenyy.common.commands;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CooldownManager {
    private final Map<String, Long> lastUsedAtMillis = new ConcurrentHashMap<>();

    public boolean tryAcquire(String key, int cooldownSeconds) {
        if (cooldownSeconds <= 0) {
            return true;
        }
        long now = System.currentTimeMillis();
        long cooldownMillis = cooldownSeconds * 1000L;

        boolean[] acquired = {false};
        lastUsedAtMillis.compute(key, (ignored, lastUsed) -> {
            if (lastUsed == null || now - lastUsed >= cooldownMillis) {
                acquired[0] = true;
                return now;
            }
            acquired[0] = false;
            return lastUsed;
        });
        return acquired[0];
    }

    public long remainingSeconds(String key, int cooldownSeconds) {
        Long lastUsed = lastUsedAtMillis.get(key);
        if (lastUsed == null) {
            return 0;
        }
        long elapsedMillis = System.currentTimeMillis() - lastUsed;
        long remainingMillis = (cooldownSeconds * 1000L) - elapsedMillis;
        return remainingMillis <= 0 ? 0 : (remainingMillis + 999) / 1000;
    }

    public void reset(String key) {
        lastUsedAtMillis.remove(key);
    }
}
