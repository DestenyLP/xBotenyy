package de.destenylp.xBotenyy.launcher.bot;

import java.util.Locale;
import java.util.Optional;

public enum BotId {
    DISCORD("discord", "discordbot", "dc", "d"),
    TWITCH("twitch", "twitchbot", "tw", "t");

    private final String[] aliases;

    BotId(String... aliases) {
        this.aliases = aliases;
    }

    public static Optional<BotId> parse(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String normalized = token.trim().toLowerCase(Locale.ROOT);
        for (BotId id : values()) {
            for (String alias : id.aliases) {
                if (alias.equals(normalized)) {
                    return Optional.of(id);
                }
            }
        }
        return Optional.empty();
    }

    public String primaryName() {
        return aliases[0];
    }
}
