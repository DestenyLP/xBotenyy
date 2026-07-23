package de.destenylp.xBotenyy.discordbot.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DiscordTimestamps {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private DiscordTimestamps() {
    }

    public static String format(long epochMillis) {
        return FORMATTER.format(Instant.ofEpochMilli(epochMillis));
    }
}
