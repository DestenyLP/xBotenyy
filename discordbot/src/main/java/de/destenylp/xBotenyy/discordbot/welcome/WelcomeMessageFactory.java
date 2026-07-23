package de.destenylp.xBotenyy.discordbot.welcome;

import de.destenylp.xBotenyy.discordbot.messaging.MessageRenderer;
import de.destenylp.xBotenyy.discordbot.messaging.RenderedMessage;
import de.destenylp.xBotenyy.discordbot.placeholders.PlaceholderContext;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public final class WelcomeMessageFactory {
    private WelcomeMessageFactory() {
    }

    public static RenderedMessage build(WelcomeVariant variant, Member member, Guild guild, TextChannel welcomeChannel) {
        PlaceholderContext context = PlaceholderContext.of(member, guild, welcomeChannel);
        RenderedMessage rendered = MessageRenderer.render(variant.getTemplate(), context);

        if (!variant.isPing() || member == null) {
            return rendered;
        }

        String mention = member.getAsMention();
        if (rendered.embed() != null) {
            return new RenderedMessage(mention, rendered.embed());
        }

        String combinedContent = rendered.content() != null ? mention + "\n" + rendered.content() : mention;
        return new RenderedMessage(combinedContent, null);
    }
}
