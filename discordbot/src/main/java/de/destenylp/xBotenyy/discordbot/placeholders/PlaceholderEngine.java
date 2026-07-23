package de.destenylp.xBotenyy.discordbot.placeholders;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class PlaceholderEngine {
    private static final Map<String, Function<PlaceholderContext, String>> RESOLVERS = new ConcurrentHashMap<>();

    private PlaceholderEngine() {
    }

    public static void register(String key, Function<PlaceholderContext, String> resolver) {
        RESOLVERS.put(key, resolver);
    }

    public static String apply(String text, PlaceholderContext context) {
        if (text == null) {
            return null;
        }

        String result = text.replace("\\n", "\n");

        for (Map.Entry<String, Function<PlaceholderContext, String>> resolver : RESOLVERS.entrySet()) {
            String value = resolver.getValue().apply(context);
            if (value != null) {
                result = result.replace("{" + resolver.getKey() + "}", value);
            }
        }

        for (Map.Entry<String, String> entry : context.getValues().entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return result;
    }
}
