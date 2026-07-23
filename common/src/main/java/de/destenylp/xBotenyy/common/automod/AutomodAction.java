package de.destenylp.xBotenyy.common.automod;

public enum AutomodAction {
    NONE(0, "Nur protokollieren"),
    DELETE(1, "Nachricht löschen"),
    WARN(2, "Löschen + Verwarnung per DM"),
    TIMEOUT(3, "Löschen + Timeout"),
    KICK(4, "Löschen + Kick"),
    BAN(5, "Löschen + Bann");

    private final int severity;
    private final String label;

    AutomodAction(int severity, String label) {
        this.severity = severity;
        this.label = label;
    }

    public int getSeverity() {
        return severity;
    }

    public String getLabel() {
        return label;
    }

    public boolean deletesMessage() {
        return this != NONE;
    }

    public static AutomodAction max(AutomodAction first, AutomodAction second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.severity >= second.severity ? first : second;
    }

    public static AutomodAction fromKey(String key, AutomodAction fallback) {
        if (key == null || key.isBlank()) {
            return fallback;
        }
        try {
            return AutomodAction.valueOf(key.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
