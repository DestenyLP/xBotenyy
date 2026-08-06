package de.destenylp.xBotenyy.twitchbot.discordlog;
import de.destenylp.xBotenyy.common.automod.AutomodAction;
import de.destenylp.xBotenyy.common.automod.AutomodVerdict;
import de.destenylp.xBotenyy.common.discord.DiscordWebhookClient;
import de.destenylp.xBotenyy.twitchbot.chat.TwitchChatMessage;
public class TwitchDiscordLogService {
    private final DiscordWebhookClient webhookClient;
    private final TwitchDiscordLogSettings settings;
    public TwitchDiscordLogService(DiscordWebhookClient webhookClient, TwitchDiscordLogSettings settings) {
        this.webhookClient = webhookClient;
        this.settings = settings;
    }
    private boolean configured() {
        return settings.isConfigured();
    }
    public void logChatMessage(TwitchChatMessage message) {
        if (!configured() || !settings.messagesEnabled()) {
            return;
        }
        webhookClient.sendEmbedAsync(settings.webhookUrl(), TwitchDiscordEmbedFactory.buildChatMessage(message));
    }
    public void logOwnAutomodAction(TwitchChatMessage message, AutomodVerdict verdict, AutomodAction action, int strikes) {
        if (!configured() || !settings.automodEnabled()) {
            return;
        }
        webhookClient.sendEmbedAsync(settings.webhookUrl(),
                TwitchDiscordEmbedFactory.buildOwnAutomodAction(message, verdict, action, strikes));
    }
    public void logNativeAutomodHold(String channelLogin, String userLogin, String messageText, String category, String level) {
        if (!configured() || !settings.automodEnabled()) {
            return;
        }
        webhookClient.sendEmbedAsync(settings.webhookUrl(),
                TwitchDiscordEmbedFactory.buildNativeAutomodHold(channelLogin, userLogin, messageText, category, level));
    }
    public void logNativeAutomodUpdate(String channelLogin, String userLogin, String status, String moderatorLogin) {
        if (!configured() || !settings.automodEnabled()) {
            return;
        }
        webhookClient.sendEmbedAsync(settings.webhookUrl(),
                TwitchDiscordEmbedFactory.buildNativeAutomodUpdate(channelLogin, userLogin, status, moderatorLogin));
    }
    public void logCommandUsage(TwitchChatMessage message, String commandName, String result) {
        if (!configured() || !settings.commandsEnabled()) {
            return;
        }
        webhookClient.sendEmbedAsync(settings.webhookUrl(),
                TwitchDiscordEmbedFactory.buildCommandUsage(message, commandName, result));
    }
}
