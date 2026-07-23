package de.destenylp.xBotenyy.discordbot.util;

import java.util.Optional;

public final class DiscordLimits {
    public static final int EMBED_TITLE_MAX = 256;
    public static final int EMBED_DESCRIPTION_MAX = 4096;
    public static final int EMBED_FOOTER_MAX = 2048;
    public static final int EMBED_AUTHOR_MAX = 256;
    public static final int EMBED_FIELD_NAME_MAX = 256;
    public static final int EMBED_FIELD_VALUE_MAX = 1024;
    public static final int EMBED_MAX_FIELDS = 25;
    public static final int MESSAGE_CONTENT_MAX = 2000;

    private DiscordLimits() {
    }

    public static Optional<String> validateMessageContent(String content) {
        if (content != null && content.length() > MESSAGE_CONTENT_MAX) {
            return Optional.of("Der Nachrichtentext ist zu lang (" + content.length() + "/" + MESSAGE_CONTENT_MAX + " Zeichen).");
        }
        return Optional.empty();
    }

    public static Optional<String> validateEmbedTitle(String title) {
        if (title != null && title.length() > EMBED_TITLE_MAX) {
            return Optional.of("Der Titel ist zu lang (" + title.length() + "/" + EMBED_TITLE_MAX + " Zeichen).");
        }
        return Optional.empty();
    }

    public static Optional<String> validateEmbedDescription(String description) {
        if (description != null && description.length() > EMBED_DESCRIPTION_MAX) {
            return Optional.of("Der Beschreibungstext ist zu lang (" + description.length() + "/" + EMBED_DESCRIPTION_MAX + " Zeichen).");
        }
        return Optional.empty();
    }

    public static Optional<String> validateEmbedFooter(String footer) {
        if (footer != null && footer.length() > EMBED_FOOTER_MAX) {
            return Optional.of("Der Footer ist zu lang (" + footer.length() + "/" + EMBED_FOOTER_MAX + " Zeichen).");
        }
        return Optional.empty();
    }

    public static Optional<String> validateEmbedAuthor(String author) {
        if (author != null && author.length() > EMBED_AUTHOR_MAX) {
            return Optional.of("Der Autor-Text ist zu lang (" + author.length() + "/" + EMBED_AUTHOR_MAX + " Zeichen).");
        }
        return Optional.empty();
    }

    public static Optional<String> validateFieldCount(int fieldCount) {
        if (fieldCount > EMBED_MAX_FIELDS) {
            return Optional.of("Es sind maximal " + EMBED_MAX_FIELDS + " Felder pro Embed möglich (" + fieldCount + " angegeben).");
        }
        return Optional.empty();
    }
}
