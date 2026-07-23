package de.destenylp.xBotenyy.discordbot.tickets;

import de.destenylp.xBotenyy.discordbot.core.EntityStatus;

import java.awt.Color;

public enum TicketStatus implements EntityStatus {
    OPEN("Offen", "\uD83D\uDD35", new Color(88, 101, 242)),
    CLAIMED("In Bearbeitung", "\uD83D\uDFE1", new Color(250, 166, 26)),
    ON_HOLD("Wartet auf Antwort", "\u23F8", new Color(153, 170, 181)),
    CLOSED("Geschlossen", "\u26AB", new Color(153, 45, 34));

    private final String label;
    private final String emoji;
    private final Color color;

    TicketStatus(String label, String emoji, Color color) {
        this.label = label;
        this.emoji = emoji;
        this.color = color;
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

    public boolean isClosed() {
        return this == CLOSED;
    }
}
