package de.destenylp.xBotenyy.discordbot.util;

import java.awt.Color;
import java.util.Optional;

public final class DiscordColors {
    public static final Color BLURPLE = new Color(88, 101, 242);

    private static volatile Color brand = BLURPLE;

    private DiscordColors() {
    }

    public static void configure(Color brandColor) {
        brand = brandColor != null ? brandColor : BLURPLE;
    }

    public static Color brand() {
        return brand;
    }

    public static Optional<Color> parse(String hex) {
        if (hex == null || hex.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Color.decode(hex.startsWith("#") ? hex : "#" + hex));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static Color parseOrDefault(String hex, Color fallback) {
        return parse(hex).orElse(fallback);
    }
}
