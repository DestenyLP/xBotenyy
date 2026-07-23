package de.destenylp.xBotenyy.common.automod;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class AutomodSettingsFactory {
    private AutomodSettingsFactory() {
    }

    public static AutomodSettings from(Function<String, String> resolver) {
        return new AutomodSettings(
                bool(resolver, "automod.enabled", true),
                stringSet(resolver, "automod.exempt.role.ids"),
                stringSet(resolver, "automod.exempt.channel.ids"),
                bool(resolver, "automod.bypass.manage-server", true),
                bool(resolver, "automod.bypass.administrator", true),
                string(resolver, "automod.log.channel.id", ""),
                new AutomodSettings.WordFilterConfig(
                        bool(resolver, "automod.words.enabled", true),
                        AutomodAction.fromKey(string(resolver, "automod.words.action", "DELETE"), AutomodAction.DELETE),
                        stringSet(resolver, "automod.words.banned")),
                new AutomodSettings.InviteFilterConfig(
                        bool(resolver, "automod.invites.enabled", true),
                        stringSet(resolver, "automod.invites.whitelist-codes"),
                        AutomodAction.fromKey(string(resolver, "automod.invites.action", "DELETE"), AutomodAction.DELETE)),
                new AutomodSettings.MentionFilterConfig(
                        bool(resolver, "automod.mentions.enabled", true),
                        intVal(resolver, "automod.mentions.max", 5),
                        AutomodAction.fromKey(string(resolver, "automod.mentions.action", "DELETE"), AutomodAction.DELETE)),
                new AutomodSettings.CapsFilterConfig(
                        bool(resolver, "automod.caps.enabled", true),
                        intVal(resolver, "automod.caps.min-length", 15),
                        intVal(resolver, "automod.caps.max-percentage", 70),
                        AutomodAction.fromKey(string(resolver, "automod.caps.action", "DELETE"), AutomodAction.DELETE)),
                new AutomodSettings.SpamFilterConfig(
                        bool(resolver, "automod.spam.enabled", true),
                        intVal(resolver, "automod.spam.max-messages", 5),
                        intVal(resolver, "automod.spam.window-seconds", 6),
                        AutomodAction.fromKey(string(resolver, "automod.spam.action", "TIMEOUT"), AutomodAction.TIMEOUT)),
                new AutomodSettings.DuplicateFilterConfig(
                        bool(resolver, "automod.duplicate.enabled", true),
                        intVal(resolver, "automod.duplicate.max-repeats", 3),
                        AutomodAction.fromKey(string(resolver, "automod.duplicate.action", "DELETE"), AutomodAction.DELETE)),
                new AutomodSettings.LinkFilterConfig(
                        bool(resolver, "automod.links.enabled", false),
                        stringSet(resolver, "automod.links.whitelist-domains"),
                        AutomodAction.fromKey(string(resolver, "automod.links.action", "DELETE"), AutomodAction.DELETE)),
                new AutomodSettings.AiFilterConfig(
                        bool(resolver, "automod.ai.enabled", false),
                        doubleVal(resolver, "automod.ai.threshold", 0.85),
                        intVal(resolver, "automod.ai.timeout.seconds", 5),
                        AutomodAction.fromKey(string(resolver, "automod.ai.action", "DELETE"), AutomodAction.DELETE)),
                new AutomodSettings.StrikeConfig(
                        bool(resolver, "automod.strikes.enabled", true),
                        intVal(resolver, "automod.strikes.expiry.minutes", 60),
                        intVal(resolver, "automod.strikes.timeout.threshold", 3),
                        intVal(resolver, "automod.strikes.timeout.duration.minutes", 10),
                        intVal(resolver, "automod.strikes.kick.threshold", 5),
                        intVal(resolver, "automod.strikes.ban.threshold", 8)));
    }

    public static Map<String, String> defaultValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("automod.enabled", "true");
        values.put("automod.log.channel.id", "");
        values.put("automod.exempt.role.ids", "");
        values.put("automod.exempt.channel.ids", "");
        values.put("automod.bypass.manage-server", "true");
        values.put("automod.bypass.administrator", "true");
        values.put("automod.words.enabled", "true");
        values.put("automod.words.action", "DELETE");
        values.put("automod.words.banned", "");
        values.put("automod.invites.enabled", "true");
        values.put("automod.invites.action", "DELETE");
        values.put("automod.invites.whitelist-codes", "");
        values.put("automod.mentions.enabled", "true");
        values.put("automod.mentions.max", "5");
        values.put("automod.mentions.action", "DELETE");
        values.put("automod.caps.enabled", "true");
        values.put("automod.caps.min-length", "15");
        values.put("automod.caps.max-percentage", "70");
        values.put("automod.caps.action", "DELETE");
        values.put("automod.spam.enabled", "true");
        values.put("automod.spam.max-messages", "5");
        values.put("automod.spam.window-seconds", "6");
        values.put("automod.spam.action", "TIMEOUT");
        values.put("automod.duplicate.enabled", "true");
        values.put("automod.duplicate.max-repeats", "3");
        values.put("automod.duplicate.action", "DELETE");
        values.put("automod.links.enabled", "false");
        values.put("automod.links.action", "DELETE");
        values.put("automod.links.whitelist-domains", "discord.com,discord.gg,tenor.com,giphy.com,youtube.com,youtu.be");
        values.put("automod.ai.enabled", "false");
        values.put("automod.ai.threshold", "0.85");
        values.put("automod.ai.timeout.seconds", "5");
        values.put("automod.ai.action", "DELETE");
        values.put("automod.strikes.enabled", "true");
        values.put("automod.strikes.expiry.minutes", "60");
        values.put("automod.strikes.timeout.threshold", "3");
        values.put("automod.strikes.timeout.duration.minutes", "10");
        values.put("automod.strikes.kick.threshold", "5");
        values.put("automod.strikes.ban.threshold", "8");
        return values;
    }

    private static String string(Function<String, String> resolver, String key, String fallback) {
        String value = resolver.apply(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static boolean bool(Function<String, String> resolver, String key, boolean fallback) {
        String value = resolver.apply(key);
        return value == null || value.isBlank() ? fallback : Boolean.parseBoolean(value.trim());
    }

    private static int intVal(Function<String, String> resolver, String key, int fallback) {
        try {
            String value = resolver.apply(key);
            return value == null || value.isBlank() ? fallback : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static double doubleVal(Function<String, String> resolver, String key, double fallback) {
        try {
            String value = resolver.apply(key);
            return value == null || value.isBlank() ? fallback : Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Set<String> stringSet(Function<String, String> resolver, String key) {
        String raw = resolver.apply(key);
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isBlank()) {
                result.add(trimmed);
            }
        }
        return result;
    }
}
