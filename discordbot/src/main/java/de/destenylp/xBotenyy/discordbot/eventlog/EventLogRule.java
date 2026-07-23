package de.destenylp.xBotenyy.discordbot.eventlog;

public class EventLogRule {
    private boolean enabled;
    private String channelId;

    public EventLogRule() {
    }

    public EventLogRule(boolean enabled, String channelId) {
        this.enabled = enabled;
        this.channelId = channelId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }
}
