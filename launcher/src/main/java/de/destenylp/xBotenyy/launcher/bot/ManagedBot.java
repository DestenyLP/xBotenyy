package de.destenylp.xBotenyy.launcher.bot;

public interface ManagedBot {

    BotId getId();

    String getDisplayName();

    BotStatus getStatus();

    void start();

    boolean stop(long timeoutSeconds);

    int getRestartCount();

    long getLastStartedAtMillis();
}
