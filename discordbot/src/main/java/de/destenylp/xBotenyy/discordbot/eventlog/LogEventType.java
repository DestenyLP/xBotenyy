package de.destenylp.xBotenyy.discordbot.eventlog;

import java.awt.Color;
import java.util.Optional;

public enum LogEventType {
    MEMBER_JOIN("member-join", "Mitglied beigetreten", "\uD83D\uDCE5", new Color(87, 242, 135)),
    MEMBER_LEAVE("member-leave", "Mitglied verlassen", "\uD83D\uDCE4", new Color(237, 66, 69)),
    MEMBER_BOOST("member-boost", "Server geboostet", "\uD83D\uDC8E", new Color(244, 127, 255)),
    MEMBER_UNBOOST("member-unboost", "Boost entfernt", "\uD83D\uDC94", new Color(153, 153, 153)),
    MEMBER_BAN("member-ban", "Mitglied gebannt", "\uD83D\uDD28", new Color(153, 45, 34)),
    MEMBER_UNBAN("member-unban", "Bann aufgehoben", "\u2696\uFE0F", new Color(87, 242, 135)),
    NICKNAME_CHANGE("nickname-change", "Nickname geändert", "\u270F\uFE0F", new Color(88, 101, 242)),
    ROLE_UPDATE("role-update", "Rollen aktualisiert", "\uD83C\uDFAD", new Color(88, 101, 242)),
    VOICE_UPDATE("voice-update", "Voice-Aktivität", "\uD83D\uDD0A", new Color(250, 166, 26)),
    MEMBER_TIMEOUT("member-timeout", "Mitglied Timeout", "\uD83D\uDD07", new Color(250, 166, 26)),
    MEMBER_TIMEOUT_REMOVED("member-timeout-removed", "Timeout aufgehoben", "\uD83D\uDD0A", new Color(87, 242, 135)),
    CHANNEL_CREATE("channel-create", "Channel erstellt", "\u2795", new Color(87, 242, 135)),
    CHANNEL_DELETE("channel-delete", "Channel gelöscht", "\u2796", new Color(237, 66, 69)),
    MESSAGE_DELETE("message-delete", "Nachricht gelöscht", "\uD83D\uDDD1\uFE0F", new Color(237, 66, 69));

    private final String key;
    private final String label;
    private final String emoji;
    private final Color color;

    LogEventType(String key, String label, String emoji, Color color) {
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

    public static Optional<LogEventType> fromKey(String key) {
        if (key == null) {
            return Optional.empty();
        }
        for (LogEventType type : values()) {
            if (type.key.equalsIgnoreCase(key)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
