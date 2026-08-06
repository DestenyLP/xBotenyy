package de.destenylp.xBotenyy.twitchbot.poll;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public final class TwitchPoll {
    private final String question;
    private final List<String> options;
    private final String startedBy;
    private final long startedAtEpochMillis;
    private final Map<String, Integer> votesByUserId = new HashMap<>();
    public TwitchPoll(String question, List<String> options, String startedBy, long startedAtEpochMillis) {
        this.question = question;
        this.options = List.copyOf(options);
        this.startedBy = startedBy;
        this.startedAtEpochMillis = startedAtEpochMillis;
    }
    public String question() {
        return question;
    }
    public List<String> options() {
        return options;
    }
    public String startedBy() {
        return startedBy;
    }
    public long startedAtEpochMillis() {
        return startedAtEpochMillis;
    }
    public boolean isValidOption(int optionNumber) {
        return optionNumber >= 1 && optionNumber <= options.size();
    }
    public synchronized boolean vote(String userId, int optionNumber) {
        if (!isValidOption(optionNumber)) {
            return false;
        }
        votesByUserId.put(userId, optionNumber);
        return true;
    }
    public synchronized int totalVotes() {
        return votesByUserId.size();
    }
    public synchronized List<Integer> tally() {
        List<Integer> result = new ArrayList<>(options.size());
        for (int i = 0; i < options.size(); i++) {
            result.add(0);
        }
        for (int optionNumber : votesByUserId.values()) {
            result.set(optionNumber - 1, result.get(optionNumber - 1) + 1);
        }
        return result;
    }
    public String formatOptions() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < options.size(); i++) {
            if (i > 0) {
                builder.append(" | ");
            }
            builder.append(i + 1).append(") ").append(options.get(i));
        }
        return builder.toString();
    }
    public String formatResults() {
        List<Integer> tally = tally();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < options.size(); i++) {
            if (i > 0) {
                builder.append(" | ");
            }
            builder.append(i + 1).append(") ").append(options.get(i)).append(": ").append(tally.get(i));
        }
        return builder.toString();
    }
}
