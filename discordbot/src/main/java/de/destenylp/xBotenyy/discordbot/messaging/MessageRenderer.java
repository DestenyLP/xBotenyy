package de.destenylp.xBotenyy.discordbot.messaging;

import de.destenylp.xBotenyy.discordbot.placeholders.PlaceholderContext;
import de.destenylp.xBotenyy.discordbot.placeholders.PlaceholderEngine;
import de.destenylp.xBotenyy.discordbot.util.DiscordColors;
import de.destenylp.xBotenyy.discordbot.util.DiscordLimits;
import net.dv8tion.jda.api.EmbedBuilder;

import java.time.Instant;
import java.util.Optional;

public final class MessageRenderer {
    private MessageRenderer() {
    }

    public static RenderedMessage render(MessageTemplate template, PlaceholderContext context) {
        String content = PlaceholderEngine.apply(template.getContent(), context);

        if (!template.isEmbed()) {
            return new RenderedMessage(content, null);
        }

        String title = PlaceholderEngine.apply(template.getTitle(), context);
        String titleUrl = PlaceholderEngine.apply(template.getTitleUrl(), context);
        String author = PlaceholderEngine.apply(template.getAuthor(), context);
        String footer = PlaceholderEngine.apply(template.getFooter(), context);
        String imageUrl = PlaceholderEngine.apply(template.getImageUrl(), context);

        EmbedBuilder embedBuilder = new EmbedBuilder();

        if (author != null && !author.isBlank()) {
            embedBuilder.setAuthor(truncate(author, DiscordLimits.EMBED_AUTHOR_MAX));
        }

        if (title != null && !title.isBlank()) {
            String truncatedTitle = truncate(title, DiscordLimits.EMBED_TITLE_MAX);
            if (titleUrl != null && !titleUrl.isBlank()) {
                try {
                    embedBuilder.setTitle(truncatedTitle, titleUrl);
                } catch (IllegalArgumentException e) {
                    embedBuilder.setTitle(truncatedTitle);
                }
            } else {
                embedBuilder.setTitle(truncatedTitle);
            }
        }

        if (content != null && !content.isBlank()) {
            embedBuilder.setDescription(truncate(content, DiscordLimits.EMBED_DESCRIPTION_MAX));
        }

        embedBuilder.setColor(DiscordColors.parseOrDefault(template.getColor(), DiscordColors.brand()));

        if (imageUrl != null && !imageUrl.isBlank()) {
            try {
                embedBuilder.setImage(imageUrl);
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (footer != null && !footer.isBlank()) {
            embedBuilder.setFooter(truncate(footer, DiscordLimits.EMBED_FOOTER_MAX));
        }

        if (template.isTimestamp()) {
            embedBuilder.setTimestamp(Instant.now());
        }

        return new RenderedMessage(null, embedBuilder.build());
    }

    public static Optional<String> validate(MessageTemplate template, PlaceholderContext context) {
        String content = PlaceholderEngine.apply(template.getContent(), context);

        if (!template.isEmbed()) {
            return DiscordLimits.validateMessageContent(content);
        }

        String title = PlaceholderEngine.apply(template.getTitle(), context);
        String author = PlaceholderEngine.apply(template.getAuthor(), context);
        String footer = PlaceholderEngine.apply(template.getFooter(), context);

        Optional<String> descriptionError = DiscordLimits.validateEmbedDescription(content);
        if (descriptionError.isPresent()) {
            return descriptionError;
        }

        Optional<String> titleError = DiscordLimits.validateEmbedTitle(title);
        if (titleError.isPresent()) {
            return titleError;
        }

        Optional<String> authorError = DiscordLimits.validateEmbedAuthor(author);
        if (authorError.isPresent()) {
            return authorError;
        }

        return DiscordLimits.validateEmbedFooter(footer);
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
