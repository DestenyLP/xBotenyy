package de.destenylp.xBotenyy.discordbot.raidprotection;

import de.destenylp.xBotenyy.discordbot.core.GuildService;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class RaidProtectionService implements GuildService {
    private final RaidProtectionRepository repository;
    private final RaidProtectionConfig config;
    private final Map<String, Deque<Long>> joinTimestamps = new ConcurrentHashMap<>();
    private final Map<String, Long> raidModeUntil = new ConcurrentHashMap<>();

    public RaidProtectionService(RaidProtectionRepository repository, RaidProtectionConfig config) {
        this.repository = repository;
        this.config = config;
    }

    @Override
    public String getServiceName() {
        return "Raid-Schutz";
    }

    public RaidProtectionConfig getConfig() {
        return config;
    }

    public RaidProtectionSettings getSettings(String guildId) {
        return repository.getOrEmpty(guildId);
    }

    public boolean isEnabled(String guildId) {
        return repository.getOrEmpty(guildId).enabled();
    }

    public void setEnabled(String guildId, boolean enabled) {
        repository.setEnabled(guildId, enabled);
        if (!enabled) {
            joinTimestamps.remove(guildId);
            raidModeUntil.remove(guildId);
        }
    }

    public void setAlertChannel(String guildId, String channelId) {
        repository.setAlertChannel(guildId, channelId);
    }

    public boolean registerJoinAndCheckRaidTriggered(String guildId) {
        long now = System.currentTimeMillis();
        long windowStart = now - (config.joinWindowSeconds() * 1000L);
        Deque<Long> timestamps = joinTimestamps.computeIfAbsent(guildId, id -> new ConcurrentLinkedDeque<>());
        timestamps.addLast(now);
        while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
            timestamps.pollFirst();
        }
        if (timestamps.size() >= config.joinThreshold() && !isRaidModeActive(guildId)) {
            raidModeUntil.put(guildId, now + (config.raidModeDurationSeconds() * 1000L));
            return true;
        }
        return false;
    }

    public boolean isRaidModeActive(String guildId) {
        Long until = raidModeUntil.get(guildId);
        if (until == null) {
            return false;
        }
        if (until < System.currentTimeMillis()) {
            raidModeUntil.remove(guildId);
            return false;
        }
        return true;
    }

    public long getRaidModeRemainingSeconds(String guildId) {
        Long until = raidModeUntil.get(guildId);
        if (until == null) {
            return 0;
        }
        long remaining = until - System.currentTimeMillis();
        return Math.max(0, remaining / 1000);
    }
}
