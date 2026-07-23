package de.destenylp.xBotenyy.discordbot.listeners;

import de.destenylp.xBotenyy.common.automod.AutomodAction;
import de.destenylp.xBotenyy.discordbot.automod.AutomodEmbedFactory;
import de.destenylp.xBotenyy.discordbot.automod.AutomodService;
import de.destenylp.xBotenyy.common.automod.AutomodVerdict;
import de.destenylp.xBotenyy.discordbot.observability.BotMetrics;
import de.destenylp.xBotenyy.common.util.AuditLog;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class AutomodListener extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutomodListener.class);

    private final AutomodService service;

    public AutomodListener(AutomodService service) {
        this.service = service;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild()) {
            return;
        }
        try {
            handleMessage(event.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unerwarteter Fehler im AutoMod für Guild {}: ", event.getGuild().getId(), e);
        }
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        if (!event.isFromGuild()) {
            return;
        }
        try {
            handleMessage(event.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unerwarteter Fehler im AutoMod für Guild {}: ", event.getGuild().getId(), e);
        }
    }

    private void handleMessage(Message message) {
        if (!service.getSettings().isEnabled()) {
            return;
        }
        if (message.getAuthor().isBot() || message.isWebhookMessage()) {
            return;
        }
        Guild guild = message.getGuild();
        Member member = message.getMember();
        if (member == null || service.isExempt(member)) {
            return;
        }
        if (service.isChannelExempt(message.getChannel().getId())) {
            return;
        }

        Optional<AutomodVerdict> verdict = service.evaluate(message, guild.getId(), member.getId());
        if (verdict.isPresent()) {
            applyVerdict(message, guild, member, verdict.get());
            return;
        }

        service.evaluateAiAsync(message, aiVerdict -> aiVerdict.ifPresent(v -> applyVerdict(message, guild, member, v)));
    }

    private void applyVerdict(Message message, Guild guild, Member member, AutomodVerdict verdict) {
        AutomodAction finalAction = service.registerViolationAndEscalate(guild.getId(), member.getId(), verdict.action());
        int strikes = service.getCurrentStrikes(guild.getId(), member.getId());

        if (finalAction.deletesMessage()) {
            message.delete().queue(success -> {
            }, failure -> LOGGER.warn("Konnte AutoMod-Nachricht {} nicht löschen: {}", message.getId(), failure.getMessage()));
        }

        String reason = "AutoMod: " + verdict.reason();

        switch (finalAction) {
            case WARN -> notifyMember(member, verdict, guild);
            case TIMEOUT -> member.timeoutFor(service.getTimeoutDuration()).reason(reason).queue(
                    success -> {
                    }, failure -> LOGGER.warn("Konnte Mitglied {} nicht timeouten: {}", member.getId(), failure.getMessage()));
            case KICK -> guild.kick(member).reason(reason).queue(
                    success -> {
                    }, failure -> LOGGER.warn("Konnte Mitglied {} nicht kicken: {}", member.getId(), failure.getMessage()));
            case BAN -> guild.ban(member, 0, TimeUnit.SECONDS).reason(reason).queue(
                    success -> {
                    }, failure -> LOGGER.warn("Konnte Mitglied {} nicht bannen: {}", member.getId(), failure.getMessage()));
            default -> {
            }
        }

        LOGGER.info("AutoMod: {} durch {} in Guild {} (Aktion: {}, Strikes: {})",
                verdict.ruleType(), member.getId(), guild.getId(), finalAction, strikes);
        AuditLog.record(guild.getId(), member.getId(), "AUTOMOD_" + verdict.ruleType(),
                "action=" + finalAction + " strikes=" + strikes + " reason=" + verdict.reason());
        BotMetrics.incrementAutomodViolationsDetected();

        logToChannel(message, member, verdict, finalAction, strikes);
    }

    private void notifyMember(Member member, AutomodVerdict verdict, Guild guild) {
        member.getUser().openPrivateChannel().queue(
                privateChannel -> privateChannel.sendMessage("⚠️ Deine Nachricht auf **" + guild.getName()
                        + "** wurde durch AutoMod entfernt.\nGrund: " + verdict.reason()).queue(
                        success -> {
                        }, failure -> LOGGER.warn("Konnte Mitglied {} nicht per DM verwarnen: {}", member.getId(), failure.getMessage())),
                failure -> LOGGER.warn("Konnte keinen DM-Kanal zu {} öffnen: {}", member.getId(), failure.getMessage()));
    }

    private void logToChannel(Message message, Member member, AutomodVerdict verdict, AutomodAction finalAction, int strikes) {
        String logChannelId = service.getSettings().getLogChannelId();
        if (logChannelId == null || logChannelId.isBlank()) {
            return;
        }
        TextChannel logChannel = message.getJDA().getChannelById(TextChannel.class, logChannelId);
        if (logChannel == null) {
            return;
        }
        MessageChannel sourceChannel = message.getChannel();
        logChannel.sendMessageEmbeds(AutomodEmbedFactory.buildLogEmbed(member, sourceChannel, verdict, finalAction,
                strikes, message.getContentRaw())).queue(
                success -> {
                }, failure -> LOGGER.warn("Konnte AutoMod-Log nicht senden: {}", failure.getMessage()));
    }
}
