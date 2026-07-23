package de.destenylp.xBotenyy.discordbot.eventlog;

import de.destenylp.xBotenyy.discordbot.core.GuildService;

import java.util.Optional;

public class EventLogService implements GuildService {
    private final EventLogRepository manager;

    public EventLogService(EventLogRepository manager) {
        this.manager = manager;
    }

    @Override
    public String getServiceName() {
        return "Event-Log";
    }

    public Optional<EventLogSettings> getSettings(String guildId) {
        return manager.getSettingsFor(guildId);
    }

    public void setDefaultChannel(String guildId, String channelId) {
        manager.setDefaultChannel(guildId, channelId);
    }

    public void setEventEnabled(String guildId, LogEventType type, boolean enabled) {
        manager.setEventEnabled(guildId, type, enabled);
    }

    public void setEventChannel(String guildId, LogEventType type, String channelId) {
        manager.setEventChannel(guildId, type, channelId);
    }

    public void setup(String guildId, String defaultChannelId) {
        manager.enableAll(guildId, defaultChannelId);
    }

    public boolean isEnabled(EventLogSettings settings, LogEventType type) {
        if (settings == null) {
            return false;
        }
        EventLogRule rule = settings.getRule(type);
        if (rule != null) {
            return rule.isEnabled();
        }
        return settings.getDefaultChannelId() != null;
    }

    public Optional<String> resolveChannelId(String guildId, LogEventType type) {
        Optional<EventLogSettings> settingsOpt = manager.getSettingsFor(guildId);
        if (settingsOpt.isEmpty()) {
            return Optional.empty();
        }
        EventLogSettings settings = settingsOpt.get();
        if (!isEnabled(settings, type)) {
            return Optional.empty();
        }
        EventLogRule rule = settings.getRule(type);
        String channelId = rule != null && rule.getChannelId() != null ? rule.getChannelId() : settings.getDefaultChannelId();
        return Optional.ofNullable(channelId);
    }
}
