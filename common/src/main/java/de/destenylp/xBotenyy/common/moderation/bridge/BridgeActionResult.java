package de.destenylp.xBotenyy.common.moderation.bridge;

import com.google.gson.JsonObject;
import de.destenylp.xBotenyy.common.util.JsonUtil;

public record BridgeActionResult(boolean success, String message) {

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("success", success);
        json.addProperty("message", message == null ? "" : message);
        return json;
    }

    public static BridgeActionResult fromJson(JsonObject json) {
        return new BridgeActionResult(
                json.has("success") && json.get("success").getAsBoolean(),
                JsonUtil.optString(json, "message", ""));
    }
}
