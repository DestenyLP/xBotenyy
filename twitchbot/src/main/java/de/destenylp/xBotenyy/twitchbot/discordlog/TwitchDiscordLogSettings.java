package de.destenylp.xBotenyy.twitchbot.discordlog;

public record TwitchDiscordLogSettings(String webhookUrl, boolean messagesEnabled, boolean automodEnabled,
                                        boolean commandsEnabled) {
    public boolean isConfigured() {
        return webhookUrl != null && !webhookUrl.isBlank();
    }
}
