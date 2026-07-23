package de.destenylp.xBotenyy.twitchbot.broadcast;

public record TwitchBroadcastMessage(
        long id,
        String channelLogin,
        String message,
        long intervalSeconds,
        int minMessages,
        boolean enabled,
        String createdBy,
        long createdAtEpochMillis,
        long lastSentAtEpochMillis) {

    public boolean isDueAt(long nowEpochMillis, long messagesSinceLastSend) {
        if (!enabled) {
            return false;
        }
        if (messagesSinceLastSend < minMessages) {
            return false;
        }
        long elapsedSeconds = (nowEpochMillis - lastSentAtEpochMillis) / 1000;
        return elapsedSeconds >= intervalSeconds;
    }
}
