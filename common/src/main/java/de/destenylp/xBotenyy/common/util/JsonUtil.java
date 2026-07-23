package de.destenylp.xBotenyy.common.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class JsonUtil {
    private JsonUtil() {
    }

    public static String optString(JsonObject object, String key) {
        return optString(object, key, null);
    }

    public static String optString(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsString();
    }
}
