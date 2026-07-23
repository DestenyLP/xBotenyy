package de.destenylp.xBotenyy.discordbot.socials;

import de.destenylp.xBotenyy.discordbot.messaging.MessageRenderer;
import de.destenylp.xBotenyy.discordbot.messaging.RenderedMessage;
import de.destenylp.xBotenyy.discordbot.placeholders.PlaceholderContext;
import de.destenylp.xBotenyy.discordbot.socials.twitch.TwitchStream;
import de.destenylp.xBotenyy.discordbot.socials.youtube.YoutubeVideo;
import net.dv8tion.jda.api.entities.Guild;

public final class SocialMessageFactory {
    private static final String EVERYONE_PING = "@everyone";

    private SocialMessageFactory() {
    }

    public static RenderedMessage buildYoutubeMessage(SocialAccount account, YoutubeVideo video, Guild guild) {
        PlaceholderContext context = PlaceholderContext.of(guild)
                .with("account", account.getName())
                .with("video.title", video.title())
                .with("video.url", video.url())
                .with("video.thumbnail", video.thumbnailUrl());
        return withEveryonePing(MessageRenderer.render(account.getYoutubeTemplate(), context));
    }

    public static RenderedMessage buildTwitchMessage(SocialAccount account, TwitchStream stream, Guild guild) {
        PlaceholderContext context = PlaceholderContext.of(guild)
                .with("account", account.getName())
                .with("stream.title", stream.title())
                .with("stream.game", stream.gameName() != null ? stream.gameName() : "")
                .with("stream.url", stream.url())
                .with("stream.thumbnail", stream.thumbnailUrl() != null ? stream.thumbnailUrl() : "")
                .with("twitch.login", account.getTwitchLogin());
        return withEveryonePing(MessageRenderer.render(account.getTwitchTemplate(), context));
    }

    private static RenderedMessage withEveryonePing(RenderedMessage rendered) {
        String existingContent = rendered.content();
        String content = (existingContent != null && !existingContent.isBlank())
                ? EVERYONE_PING + " " + existingContent
                : EVERYONE_PING;
        return new RenderedMessage(content, rendered.embed());
    }
}
