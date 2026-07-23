package de.destenylp.xBotenyy.discordbot.reports;

import de.destenylp.xBotenyy.discordbot.core.EntityStatus;

import java.awt.Color;

public enum ReportStatus implements EntityStatus {
    OPEN("Unbearbeitet", "\uD83D\uDD34", new Color(237, 66, 69)),
    IN_PROGRESS("In Bearbeitung", "\uD83D\uDFE1", new Color(250, 166, 26)),
    RESOLVED("Fertig", "\uD83D\uDFE2", new Color(87, 242, 135)),
    REJECTED("Abgelehnt", "\u26AB", new Color(153, 45, 34));

    private final String label;
    private final String emoji;
    private final Color color;

    ReportStatus(String label, String emoji, Color color) {
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
        return this == RESOLVED || this == REJECTED;
    }
}
