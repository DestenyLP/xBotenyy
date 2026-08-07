package de.destenylp.xBotenyy.twitchbot.alerts;
import com.google.gson.JsonObject;
import de.destenylp.xBotenyy.common.discord.DiscordWebhookClient;
import de.destenylp.xBotenyy.twitchbot.chat.TwitchChatClient;
import de.destenylp.xBotenyy.twitchbot.chat.TwitchFollowEvent;
import de.destenylp.xBotenyy.twitchbot.chat.TwitchRaidEvent;
import de.destenylp.xBotenyy.twitchbot.chat.TwitchSubscribeEvent;
import java.time.Instant;
public class TwitchAlertService {
    private static final int COLOR_FOLLOW = 0x9146FF;
    private static final int COLOR_SUBSCRIBE = 0xF4A62A;
    private static final int COLOR_RAID = 0xEB4B98;
    private final TwitchChatClient chatClient;
    private final DiscordWebhookClient discordWebhookClient;
    private final String discordWebhookUrl;
    private final TwitchAlertSettings settings;
    public TwitchAlertService(TwitchChatClient chatClient, DiscordWebhookClient discordWebhookClient,
                               String discordWebhookUrl, TwitchAlertSettings settings) {
        this.chatClient = chatClient;
        this.discordWebhookClient = discordWebhookClient;
        this.discordWebhookUrl = discordWebhookUrl;
        this.settings = settings;
    }
    public void handleFollow(TwitchFollowEvent event) {
        if (settings.followChatEnabled()) {
            chatClient.sendMessage(event.channelLogin(),
                    render(settings.followChatMessage(), event.displayName(), event.channelLogin(), null, null));
        }
        if (settings.followDiscordEnabled() && discordConfigured()) {
            JsonObject embed = baseEmbed(COLOR_FOLLOW, "\uD83D\uDC9C Neuer Follower");
            embed.addProperty("description", event.displayName() + " folgt jetzt #" + event.channelLogin() + "!");
            discordWebhookClient.sendEmbedAsync(discordWebhookUrl, embed);
        }
    }
    public void handleSubscribe(TwitchSubscribeEvent event) {
        String tierLabel = formatTier(event.tier());
        if (settings.subscribeChatEnabled()) {
            chatClient.sendMessage(event.channelLogin(),
                    render(settings.subscribeChatMessage(), event.displayName(), event.channelLogin(), tierLabel, null));
        }
        if (settings.subscribeDiscordEnabled() && discordConfigured()) {
            JsonObject embed = baseEmbed(COLOR_SUBSCRIBE, "\u2B50 Neuer Sub");
            embed.addProperty("description", event.displayName() + " hat #" + event.channelLogin()
                    + " abonniert (" + tierLabel + ")" + (event.gift() ? " - geschenkt" : "") + "!");
            discordWebhookClient.sendEmbedAsync(discordWebhookUrl, embed);
        }
    }
    public void handleRaid(TwitchRaidEvent event) {
        String viewers = String.valueOf(event.viewers());
        if (settings.raidChatEnabled()) {
            chatClient.sendMessage(event.channelLogin(),
                    render(settings.raidChatMessage(), event.displayName(), event.channelLogin(), null, viewers));
        }
        if (settings.raidDiscordEnabled() && discordConfigured()) {
            JsonObject embed = baseEmbed(COLOR_RAID, "\uD83D\uDE80 Raid");
            embed.addProperty("description", event.displayName() + " raidet #" + event.channelLogin()
                    + " mit " + viewers + " Zuschauern!");
            discordWebhookClient.sendEmbedAsync(discordWebhookUrl, embed);
        }
    }
    private boolean discordConfigured() {
        return discordWebhookUrl != null && !discordWebhookUrl.isBlank();
    }
    private String render(String template, String user, String channel, String tier, String viewers) {
        String result = template.replace("{user}", user).replace("{channel}", channel);
        if (tier != null) {
            result = result.replace("{tier}", tier);
        }
        if (viewers != null) {
            result = result.replace("{viewers}", viewers);
        }
        return result;
    }
    private String formatTier(String rawTier) {
        return switch (rawTier == null ? "" : rawTier) {
            case "2000" -> "Tier 2";
            case "3000" -> "Tier 3";
            default -> "Tier 1";
        };
    }
    private JsonObject baseEmbed(int color, String title) {
        JsonObject embed = new JsonObject();
        embed.addProperty("title", title);
        embed.addProperty("color", color);
        embed.addProperty("timestamp", Instant.now().toString());
        return embed;
    }
}
