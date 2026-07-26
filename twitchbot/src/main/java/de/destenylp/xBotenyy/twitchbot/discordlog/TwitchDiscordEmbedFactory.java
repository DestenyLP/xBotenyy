package de.destenylp.xBotenyy.twitchbot.discordlog;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.destenylp.xBotenyy.common.automod.AutomodAction;
import de.destenylp.xBotenyy.common.automod.AutomodVerdict;
import de.destenylp.xBotenyy.twitchbot.chat.TwitchChatMessage;

import java.time.Instant;

public final class TwitchDiscordEmbedFactory {
    private static final int COLOR_MESSAGE = 0x5865F2;
    private static final int COLOR_OWN_AUTOMOD = 0xF4A62A;
    private static final int COLOR_NATIVE_AUTOMOD = 0xED4245;
    private static final int COLOR_COMMAND = 0x57F287;

    private TwitchDiscordEmbedFactory() {
    }

    private static JsonObject base(int color, String title) {
        JsonObject embed = new JsonObject();
        embed.addProperty("title", title);
        embed.addProperty("color", color);
        embed.addProperty("timestamp", Instant.now().toString());
        return embed;
    }

    private static JsonObject field(String name, String value, boolean inline) {
        JsonObject field = new JsonObject();
        field.addProperty("name", name);
        field.addProperty("value", value == null || value.isBlank() ? "-" : value);
        field.addProperty("inline", inline);
        return field;
    }

    private static void addFields(JsonObject embed, JsonObject... fields) {
        JsonArray array = new JsonArray();
        for (JsonObject field : fields) {
            array.add(field);
        }
        embed.add("fields", array);
    }

    private static void addFooter(JsonObject embed, String text) {
        JsonObject footer = new JsonObject();
        footer.addProperty("text", text);
        embed.add("footer", footer);
    }

    public static JsonObject buildChatMessage(TwitchChatMessage message) {
        JsonObject embed = base(COLOR_MESSAGE, "\uD83D\uDCAC Twitch-Chat-Nachricht");
        embed.addProperty("description", message.content());
        addFields(embed,
                field("Kanal", "#" + message.channelLogin(), true),
                field("Nutzer", message.displayName() + " (" + message.userLogin() + ")", true));
        addFooter(embed, "User-ID: " + message.userId());
        return embed;
    }

    public static JsonObject buildOwnAutomodAction(TwitchChatMessage message, AutomodVerdict verdict,
                                                    AutomodAction action, int strikes) {
        JsonObject embed = base(COLOR_OWN_AUTOMOD, "\u26A0\uFE0F Eigenes AutoMod");
        embed.addProperty("description", message.displayName() + " (" + message.userLogin() + ") in #" + message.channelLogin());
        addFields(embed,
                field("Regel", verdict.ruleType().name(), true),
                field("Aktion", action.name(), true),
                field("Strikes", String.valueOf(strikes), true),
                field("Grund", verdict.reason(), false),
                field("Nachricht", message.content(), false));
        addFooter(embed, "User-ID: " + message.userId());
        return embed;
    }

    public static JsonObject buildNativeAutomodHold(String channelLogin, String userLogin, String messageText,
                                                     String category, String level) {
        JsonObject embed = base(COLOR_NATIVE_AUTOMOD, "\uD83D\uDEE1\uFE0F Twitch AutoMod (nativ)");
        embed.addProperty("description", "Twitch hat eine Nachricht von **" + userLogin + "** in #" + channelLogin
                + " zur Pruefung zurueckgehalten.");
        addFields(embed,
                field("Kategorie", category, true),
                field("Level", level, true),
                field("Nachricht", messageText, false));
        return embed;
    }

    public static JsonObject buildNativeAutomodUpdate(String channelLogin, String userLogin, String status,
                                                       String moderatorLogin) {
        JsonObject embed = base(COLOR_NATIVE_AUTOMOD, "\uD83D\uDEE1\uFE0F Twitch AutoMod (nativ) \u2013 Entscheidung");
        embed.addProperty("description", "Die zurueckgehaltene Nachricht von **" + userLogin + "** in #" + channelLogin
                + " wurde " + status + ".");
        addFields(embed, field("Moderator", moderatorLogin != null ? moderatorLogin : "Twitch AutoMod", true));
        return embed;
    }

    public static JsonObject buildCommandUsage(TwitchChatMessage message, String commandName, String result) {
        JsonObject embed = base(COLOR_COMMAND, "\uD83E\uDDFE Command-Nutzung");
        embed.addProperty("description", message.displayName() + " (" + message.userLogin() + ") hat `" + commandName
                + "` in #" + message.channelLogin() + " genutzt.");
        addFields(embed, field("Status", result, true));
        return embed;
    }
}
