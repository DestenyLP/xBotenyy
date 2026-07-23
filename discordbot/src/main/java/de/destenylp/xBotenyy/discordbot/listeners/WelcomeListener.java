package de.destenylp.xBotenyy.discordbot.listeners;

import de.destenylp.xBotenyy.discordbot.messaging.MessageDispatcher;
import de.destenylp.xBotenyy.discordbot.observability.BotMetrics;
import de.destenylp.xBotenyy.discordbot.welcome.WelcomeMessageFactory;
import de.destenylp.xBotenyy.discordbot.welcome.WelcomeService;
import de.destenylp.xBotenyy.discordbot.welcome.WelcomeSettings;
import de.destenylp.xBotenyy.discordbot.welcome.WelcomeVariant;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class WelcomeListener extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(WelcomeListener.class);

    private final WelcomeService service;

    public WelcomeListener(WelcomeService service) {
        this.service = service;
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        try {
            handleGuildMemberJoin(event);
        } catch (Exception e) {
            LOGGER.error("Unerwarteter Fehler bei Willkommensnachricht für Guild {}: ", event.getGuild().getId(), e);
        }
    }

    private void handleGuildMemberJoin(GuildMemberJoinEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();

        Optional<WelcomeSettings> settingsOpt = service.getSettings(guild.getId());
        if (settingsOpt.isEmpty()) {
            return;
        }

        WelcomeSettings settings = settingsOpt.get();
        if (!settings.isEnabled() || settings.getChannelId() == null) {
            return;
        }

        Optional<WelcomeVariant> variantOpt = service.getRandomVariant(guild.getId());
        if (variantOpt.isEmpty()) {
            return;
        }

        TextChannel channel = event.getJDA().getChannelById(TextChannel.class, settings.getChannelId());
        if (channel == null) {
            LOGGER.warn("Welcome channel {} not found for guild {}", settings.getChannelId(), guild.getId());
            return;
        }

        WelcomeVariant variant = variantOpt.get();
        sendTo(channel, member, guild, channel, variant);
        LOGGER.info("Sent welcome message (variant {}) for member {} in guild {}", variant.getId(), member.getId(), guild.getId());
        BotMetrics.incrementWelcomeMessagesSent();

        if (settings.isDmEnabled()) {
            member.getUser().openPrivateChannel().queue(
                    dm -> sendTo(dm, member, guild, channel, variant),
                    failure -> LOGGER.warn("Could not send welcome DM to {}: {}", member.getId(), failure.getMessage()));
        }
    }

    private void sendTo(MessageChannel target, Member member, Guild guild, TextChannel welcomeChannel, WelcomeVariant variant) {
        MessageDispatcher.prepare(target, WelcomeMessageFactory.build(variant, member, guild, welcomeChannel))
                .ifPresent(action -> action.queue(null,
                        failure -> LOGGER.warn("Failed to send welcome message: {}", failure.getMessage())));
    }
}
