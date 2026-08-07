package de.destenylp.xBotenyy.common.moderation.bridge;

import com.google.gson.JsonObject;
import de.destenylp.xBotenyy.common.util.JsonUtil;

public record BridgeLinkConfirmResult(
        boolean success,
        String discordUserId,
        String discordUsername,
        String twitchUserId,
        String twitchLogin,
        String message) {
    public static BridgeLinkConfirmResult fromJson(JsonObject json) {
        return new BridgeLinkConfirmResult(
                json.has("success") && json.get("success").getAsBoolean(),
                JsonUtil.optString(json, "discordUserId"),
                JsonUtil.optString(json, "discordUsername"),
                JsonUtil.optString(json, "twitchUserId"),
                JsonUtil.optString(json, "twitchLogin"),
                JsonUtil.optString(json, "message", ""));
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("success", success);
        json.addProperty("discordUserId", discordUserId == null ? "" : discordUserId);
        json.addProperty("discordUsername", discordUsername == null ? "" : discordUsername);
        json.addProperty("twitchUserId", twitchUserId == null ? "" : twitchUserId);
        json.addProperty("twitchLogin", twitchLogin == null ? "" : twitchLogin);
        json.addProperty("message", message == null ? "" : message);
        return json;
    }
}

