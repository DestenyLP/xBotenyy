package de.destenylp.xBotenyy.discordbot.reports;

import java.util.Optional;

public enum ReportCategory {
    RULE_VIOLATION("regelverstoss", "\uD83D\uDEA8", "Regelverstoß melden", "Ein Mitglied verstößt gegen die Serverregeln."),
    TECHNICAL("technisch", "\uD83D\uDEE0", "Technisches Problem", "Ein Bug, Fehler oder technisches Problem melden."),
    SUPPORT("support", "\uD83D\uDCAC", "Support-Anfrage", "Eine allgemeine Frage oder ein Anliegen an das Team."),
    OTHER("sonstiges", "\uD83D\uDCC4", "Sonstiges", "Alles, was in keine andere Kategorie passt.");

    private final String key;
    private final String emoji;
    private final String label;
    private final String description;

    ReportCategory(String key, String emoji, String label, String description) {
        this.key = key;
        this.emoji = emoji;
        this.label = label;
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public static Optional<ReportCategory> fromKey(String key) {
        if (key == null) {
            return Optional.empty();
        }
        for (ReportCategory category : values()) {
            if (category.key.equalsIgnoreCase(key)) {
                return Optional.of(category);
            }
        }
        return Optional.empty();
    }
}
