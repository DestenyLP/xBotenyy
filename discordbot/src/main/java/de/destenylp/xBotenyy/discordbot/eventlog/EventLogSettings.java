package de.destenylp.xBotenyy.discordbot.eventlog;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EventLogSettings {
    private String defaultChannelId;
    private final Map<LogEventType, EventLogRule> rules = new ConcurrentHashMap<>();

    public String getDefaultChannelId() {
        return defaultChannelId;
    }

    public void setDefaultChannelId(String defaultChannelId) {
        this.defaultChannelId = defaultChannelId;
    }

    public Map<LogEventType, EventLogRule> getRules() {
        return Collections.unmodifiableMap(rules);
    }

    EventLogRule getOrCreateRule(LogEventType type) {
        return rules.computeIfAbsent(type, key -> new EventLogRule(true, null));
    }

    EventLogRule getRule(LogEventType type) {
        return rules.get(type);
    }

    void putRule(LogEventType type, EventLogRule rule) {
        rules.put(type, rule);
    }
}
