package de.destenylp.xBotenyy.discordbot.placeholders;

import java.time.format.DateTimeFormatter;

public final class DefaultPlaceholders {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private DefaultPlaceholders() {
    }

    public static void registerAll() {
        PlaceholderEngine.register("user", context -> context.getMember() != null
                ? context.getMember().getAsMention() : null);
        PlaceholderEngine.register("user.name", context -> context.getMember() != null
                ? context.getMember().getEffectiveName() : null);
        PlaceholderEngine.register("user.username", context -> context.getMember() != null
                ? context.getMember().getUser().getName() : null);
        PlaceholderEngine.register("user.id", context -> context.getMember() != null
                ? context.getMember().getId() : null);
        PlaceholderEngine.register("user.avatar", context -> context.getMember() != null
                ? context.getMember().getEffectiveAvatarUrl() : null);
        PlaceholderEngine.register("server", context -> context.getGuild() != null
                ? context.getGuild().getName() : null);
        PlaceholderEngine.register("server.id", context -> context.getGuild() != null
                ? context.getGuild().getId() : null);
        PlaceholderEngine.register("membercount", context -> context.getGuild() != null
                ? String.valueOf(context.getGuild().getMemberCount()) : null);
        PlaceholderEngine.register("channel", context -> context.getChannel() != null
                ? context.getChannel().getAsMention() : null);
        PlaceholderEngine.register("joined", context -> context.getMember() != null && context.getMember().getTimeJoined() != null
                ? context.getMember().getTimeJoined().format(DATE_FORMAT) : null);
        PlaceholderEngine.register("created", context -> context.getMember() != null && context.getMember().getUser().getTimeCreated() != null
                ? context.getMember().getUser().getTimeCreated().format(DATE_FORMAT) : null);
    }
}
