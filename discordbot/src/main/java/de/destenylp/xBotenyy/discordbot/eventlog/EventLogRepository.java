package de.destenylp.xBotenyy.discordbot.eventlog;

import java.util.Optional;

public interface EventLogRepository {
    void setDefaultChannel(String guildId, String channelId);
    void setEventEnabled(String guildId, LogEventType type, boolean enabled);
    void setEventChannel(String guildId, LogEventType type, String channelId);
    void enableAll(String guildId, String defaultChannelId);
    Optional<EventLogSettings> getSettingsFor(String guildId);
}
