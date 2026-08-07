package de.destenylp.xBotenyy.common.moderation.bridge;

import com.google.gson.JsonObject;
import de.destenylp.xBotenyy.common.util.JsonUtil;

public record BridgeLinkConfirmRequest(
        String code,
        String twitchUserId,
        String twitchLogin,
        String discordUserId,
        String discordUsername) {
    public static BridgeLinkConfirmRequest fromJson(JsonObject json) {
        return new BridgeLinkConfirmRequest(
                JsonUtil.optString(json, "code"),
                JsonUtil.optString(json, "twitchUserId"),
                JsonUtil.optString(json, "twitchLogin"),
                JsonUtil.optString(json, "discordUserId"),
                JsonUtil.optString(json, "discordUsername"));
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("code", code);
        json.addProperty("twitchUserId", twitchUserId == null ? "" : twitchUserId);
        json.addProperty("twitchLogin", twitchLogin == null ? "" : twitchLogin);
        json.addProperty("discordUserId", discordUserId == null ? "" : discordUserId);
        json.addProperty("discordUsername", discordUsername == null ? "" : discordUsername);
        return json;
    }
}

