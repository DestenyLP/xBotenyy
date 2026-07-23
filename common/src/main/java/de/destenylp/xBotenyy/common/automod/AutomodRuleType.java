package de.destenylp.xBotenyy.common.automod;

public enum AutomodRuleType {
    BANNED_WORDS("Verbotene Wörter"),
    INVITE_LINKS("Discord-Invite-Links"),
    MASS_MENTIONS("Massen-Erwähnungen"),
    EXCESSIVE_CAPS("Übermäßige Großschreibung"),
    SPAM("Nachrichten-Spam"),
    DUPLICATE_MESSAGES("Wiederholte Nachrichten"),
    LINKS("Nicht erlaubte Links"),
    AI_TOXICITY("KI-Toxizitätserkennung");

    private final String label;

    AutomodRuleType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
