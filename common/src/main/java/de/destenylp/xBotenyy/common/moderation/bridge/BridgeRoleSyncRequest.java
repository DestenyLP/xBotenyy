package de.destenylp.xBotenyy.common.moderation.bridge;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.destenylp.xBotenyy.common.moderation.TwitchRoleSyncStatus;
import de.destenylp.xBotenyy.common.util.JsonUtil;

import java.util.ArrayList;
import java.util.List;

public record BridgeRoleSyncRequest(String targetDiscordUserId, List<TwitchRoleSyncStatus> statuses) {

    public static BridgeRoleSyncRequest fromJson(JsonObject json) {
        List<TwitchRoleSyncStatus> statuses = new ArrayList<>();
        if (json.has("statuses")) {
            json.getAsJsonArray("statuses").forEach(element -> statuses.add(TwitchRoleSyncStatus.valueOf(element.getAsString())));
        }
        return new BridgeRoleSyncRequest(JsonUtil.optString(json, "targetDiscordUserId"), statuses);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("targetDiscordUserId", targetDiscordUserId);
        JsonArray array = new JsonArray();
        statuses.forEach(status -> array.add(status.name()));
        json.add("statuses", array);
        return json;
    }
}
