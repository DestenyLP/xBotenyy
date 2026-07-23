package de.destenylp.xBotenyy.twitchbot.eventlog;

import de.destenylp.xBotenyy.common.observability.Metrics;
import de.destenylp.xBotenyy.common.util.AuditLog;
import de.destenylp.xBotenyy.twitchbot.persistence.TwitchEventLogRepository;
import de.destenylp.xBotenyy.twitchbot.persistence.TwitchEventLogRepository.TwitchEventLogEntry;

import java.util.List;

public class TwitchEventLogService {
    private final TwitchEventLogRepository repository;

    public TwitchEventLogService(TwitchEventLogRepository repository) {
        this.repository = repository;
    }

    public void record(String channelLogin, String actorId, String eventType, String detail) {
        AuditLog.record(channelLogin, actorId, eventType, detail);
        repository.insert(channelLogin, eventType, actorId, detail);
        Metrics.increment("twitch.events_logged");
    }

    public List<TwitchEventLogEntry> recent(String channelLogin, int limit) {
        return repository.recent(channelLogin, limit);
    }
}
