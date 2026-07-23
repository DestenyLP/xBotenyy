package de.destenylp.xBotenyy.discordbot.giveaways;

import de.destenylp.xBotenyy.discordbot.core.EntityStatus;

import java.awt.Color;

public enum GiveawayStatus implements EntityStatus {
    RUNNING("Läuft", "\uD83C\uDF89", new Color(88, 101, 242)),
    ENDED("Beendet", "\uD83C\uDFC6", new Color(87, 242, 135)),
    CANCELLED("Abgebrochen", "\uD83D\uDEAB", new Color(153, 45, 34));

    private final String label;
    private final String emoji;
    private final Color color;

    GiveawayStatus(String label, String emoji, Color color) {
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
        return this != RUNNING;
    }
}
