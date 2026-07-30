package de.destenylp.xBotenyy.common.moderation.bridge;

import com.google.gson.JsonObject;
import de.destenylp.xBotenyy.common.moderation.ModerationAction;
import de.destenylp.xBotenyy.common.util.JsonUtil;

public record BridgeActionRequest(
        String targetUserId,
        String targetLogin,
        ModerationAction action,
        String reason,
        long durationSeconds,
        String sourceModeratorName) {

    public static BridgeActionRequest fromJson(JsonObject json) {
        return new BridgeActionRequest(
                JsonUtil.optString(json, "targetUserId"),
                JsonUtil.optString(json, "targetLogin"),
                ModerationAction.valueOf(JsonUtil.optString(json, "action")),
                JsonUtil.optString(json, "reason", ""),
                json.has("durationSeconds") ? json.get("durationSeconds").getAsLong() : 0,
                JsonUtil.optString(json, "sourceModeratorName", "Sync"));
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("targetUserId", targetUserId);
        json.addProperty("targetLogin", targetLogin);
        json.addProperty("action", action.name());
        json.addProperty("reason", reason);
        json.addProperty("durationSeconds", durationSeconds);
        json.addProperty("sourceModeratorName", sourceModeratorName);
        return json;
    }
}
