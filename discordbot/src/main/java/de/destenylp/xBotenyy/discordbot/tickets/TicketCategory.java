package de.destenylp.xBotenyy.discordbot.tickets;

import java.util.Optional;

public enum TicketCategory {
    GENERAL("allgemein", "\uD83D\uDCC4", "Allgemeine Frage", "Eine allgemeine Frage oder ein Anliegen an das Team.", 0),
    TECHNICAL("technisch", "\uD83D\uDEE0", "Technisches Problem", "Ein Bug, Fehler oder technisches Problem melden.", 0),
    SUGGESTION("vorschlag", "\uD83D\uDCA1", "Vorschlag / Feedback", "Eine Idee oder Feedback zum Server einreichen.", 0),
    APPLICATION("bewerbung", "\uD83D\uDCDD", "Team-Bewerbung", "Dich für das Team bewerben oder Fragen zur Bewerbung.", 0),
    PARTNERSHIP("partnerschaft", "\uD83E\uDD1D", "Partnerschaft", "Anfragen zu Partnerschaften oder Kooperationen.", 0),
    APPEAL("einspruch", "\u2696\uFE0F", "Einspruch / Entbannung", "Einspruch gegen eine Strafe oder Antrag auf Entbannung.", 4),
    STAFF_REPORT("teammeldung", "\u26A0\uFE0F", "Team melden", "Ein Anliegen, das vertraulich mit der Leitung besprochen werden soll.", 8),
    OTHER("sonstiges", "\u2753", "Sonstiges", "Alles, was in keine andere Kategorie passt.", 0);

    private final String key;
    private final String emoji;
    private final String label;
    private final String description;
    private final int defaultPriorityWeight;

    TicketCategory(String key, String emoji, String label, String description, int defaultPriorityWeight) {
        this.key = key;
        this.emoji = emoji;
        this.label = label;
        this.description = description;
        this.defaultPriorityWeight = defaultPriorityWeight;
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

    public TicketPriority getDefaultPriority() {
        if (defaultPriorityWeight >= 8) {
            return TicketPriority.URGENT;
        }
        if (defaultPriorityWeight >= 4) {
            return TicketPriority.HIGH;
        }
        return TicketPriority.MEDIUM;
    }

    public static Optional<TicketCategory> fromKey(String key) {
        if (key == null) {
            return Optional.empty();
        }
        for (TicketCategory category : values()) {
            if (category.key.equalsIgnoreCase(key)) {
                return Optional.of(category);
            }
        }
        return Optional.empty();
    }
}
