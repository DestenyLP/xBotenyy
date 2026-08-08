package de.destenylp.xBotenyy.discordbot.socials;

import de.destenylp.xBotenyy.discordbot.messaging.MessageRenderer;
import de.destenylp.xBotenyy.discordbot.messaging.RenderedMessage;
import de.destenylp.xBotenyy.discordbot.placeholders.PlaceholderContext;
import de.destenylp.xBotenyy.discordbot.socials.tiktok.TikTokVideo;
import de.destenylp.xBotenyy.discordbot.socials.twitch.TwitchStream;
import de.destenylp.xBotenyy.discordbot.socials.youtube.YoutubeVideo;
import net.dv8tion.jda.api.entities.Guild;

public final class SocialMessageFactory {
    private SocialMessageFactory() {
    }

    public static RenderedMessage buildYoutubeMessage(SocialAccount account, YoutubeVideo video, Guild guild) {
        PlaceholderContext context = PlaceholderContext.of(guild)
                .with("account", account.getName())
                .with("video.title", video.title())
                .with("video.url", video.url())
                .with("video.thumbnail", video.thumbnailUrl());
        return withPing(MessageRenderer.render(account.getYoutubeTemplate(), context), account.getYoutubeTemplate().getPing());
    }

    public static RenderedMessage buildTwitchMessage(SocialAccount account, TwitchStream stream, Guild guild) {
        PlaceholderContext context = PlaceholderContext.of(guild)
                .with("account", account.getName())
                .with("stream.title", stream.title())
                .with("stream.game", stream.gameName() != null ? stream.gameName() : "")
                .with("stream.url", stream.url())
                .with("stream.thumbnail", stream.thumbnailUrl() != null ? stream.thumbnailUrl() : "")
                .with("twitch.login", account.getTwitchLogin());
        return withPing(MessageRenderer.render(account.getTwitchTemplate(), context), account.getTwitchTemplate().getPing());
    }

    public static RenderedMessage buildTiktokMessage(SocialAccount account, TikTokVideo video, Guild guild) {
        PlaceholderContext context = PlaceholderContext.of(guild)
                .with("account", account.getName())
                .with("tiktok.title", video.title())
                .with("tiktok.url", video.url())
                .with("tiktok.thumbnail", video.thumbnailUrl() != null ? video.thumbnailUrl() : "");
        return withPing(MessageRenderer.render(account.getTiktokTemplate(), context), account.getTiktokTemplate().getPing());
    }

    private static RenderedMessage withPing(RenderedMessage rendered, String ping) {
        if (ping == null || ping.isBlank()) {
            return rendered;
        }
        String existingContent = rendered.content();
        String content = (existingContent != null && !existingContent.isBlank())
                ? ping + " " + existingContent
                : ping;
        return new RenderedMessage(content, rendered.embed());
    }
}

