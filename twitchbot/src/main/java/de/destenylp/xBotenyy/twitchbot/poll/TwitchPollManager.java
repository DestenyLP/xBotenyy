package de.destenylp.xBotenyy.twitchbot.poll;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class TwitchPollManager {
    private final ConcurrentMap<String, TwitchPoll> activePolls = new ConcurrentHashMap<>();

    public TwitchPoll start(String channelLogin, String question, List<String> options, String startedBy) {
        TwitchPoll poll = new TwitchPoll(question, options, startedBy, Instant.now().toEpochMilli());
        activePolls.put(channelLogin, poll);
        return poll;
    }

    public Optional<TwitchPoll> get(String channelLogin) {
        return Optional.ofNullable(activePolls.get(channelLogin));
    }

    public boolean isActive(String channelLogin) {
        return activePolls.containsKey(channelLogin);
    }

    public Optional<TwitchPoll> end(String channelLogin) {
        return Optional.ofNullable(activePolls.remove(channelLogin));
    }
}

