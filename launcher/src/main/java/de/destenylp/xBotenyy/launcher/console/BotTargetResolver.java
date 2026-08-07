package de.destenylp.xBotenyy.launcher.console;

import de.destenylp.xBotenyy.launcher.bot.BotRegistry;
import de.destenylp.xBotenyy.launcher.bot.ManagedBot;

import java.util.List;
import java.util.Locale;

public final class BotTargetResolver {
    private static final List<String> ALL_TOKENS = List.of("all", "both", "*");

    private BotTargetResolver() {
    }

    public static List<ManagedBot> resolve(String[] args, BotRegistry registry, String commandName) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Please specify a bot. Usage: "
                    + commandName + " <discord|twitch|all>");
        }
        String token = args[0].toLowerCase(Locale.ROOT);
        if (ALL_TOKENS.contains(token)) {
            if (registry.isEmpty()) {
                throw new IllegalArgumentException("No bots registered.");
            }
            return List.copyOf(registry.all());
        }
        return registry.find(token)
                .map(List::of)
                .orElseThrow(() -> new IllegalArgumentException("Unknown bot '" + args[0]
                        + "'. Valid values: discord, twitch, all."));
    }
}

