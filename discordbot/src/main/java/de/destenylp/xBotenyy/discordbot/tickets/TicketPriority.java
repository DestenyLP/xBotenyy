package de.destenylp.xBotenyy.discordbot.tickets;

import java.awt.Color;
import java.util.Optional;

public enum TicketPriority {
    LOW("low", "Niedrig", "\uD83D\uDFE2", new Color(87, 242, 135)),
    MEDIUM("medium", "Mittel", "\uD83D\uDFE1", new Color(250, 166, 26)),
    HIGH("high", "Hoch", "\uD83D\uDFE0", new Color(230, 126, 34)),
    URGENT("urgent", "Dringend", "\uD83D\uDD34", new Color(237, 66, 69));

    private final String key;
    private final String label;
    private final String emoji;
    private final Color color;

    TicketPriority(String key, String label, String emoji, Color color) {
        this.key = key;
        this.label = label;
        this.emoji = emoji;
        this.color = color;
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public String getEmoji() {
        return emoji;
    }

    public Color getColor() {
        return color;
    }

    public static Optional<TicketPriority> fromKey(String key) {
        if (key == null) {
            return Optional.empty();
        }
        for (TicketPriority priority : values()) {
            if (priority.key.equalsIgnoreCase(key)) {
                return Optional.of(priority);
            }
        }
        return Optional.empty();
    }
}
