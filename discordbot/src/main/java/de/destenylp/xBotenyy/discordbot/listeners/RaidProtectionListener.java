package de.destenylp.xBotenyy.discordbot.listeners;

import de.destenylp.xBotenyy.common.util.AuditLog;
import de.destenylp.xBotenyy.discordbot.raidprotection.RaidProtectionAction;
import de.destenylp.xBotenyy.discordbot.raidprotection.RaidProtectionConfig;
import de.destenylp.xBotenyy.discordbot.raidprotection.RaidProtectionEmbedFactory;
import de.destenylp.xBotenyy.discordbot.raidprotection.RaidProtectionService;
import de.destenylp.xBotenyy.discordbot.raidprotection.RaidProtectionSettings;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

public class RaidProtectionListener extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(RaidProtectionListener.class);
    private final RaidProtectionService service;

    public RaidProtectionListener(RaidProtectionService service) {
        this.service = service;
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        try {
            handle(event);
        } catch (Exception e) {
            LOGGER.error("Unexpected error in raid protection for guild {}: ", event.getGuild().getId(), e);
        }
    }

    private void handle(GuildMemberJoinEvent event) {
        Guild guild = event.getGuild();
        String guildId = guild.getId();
        RaidProtectionSettings settings = service.getSettings(guildId);
        if (!settings.enabled()) {
            return;
        }
        Member member = event.getMember();
        User user = member.getUser();
        RaidProtectionConfig config = service.getConfig();

        if (user.isBot() && config.botAutoKickEnabled() && !config.botWhitelistIds().contains(user.getId())) {
            executeAction(guild, member, RaidProtectionAction.KICK, config.botKickReason());
            sendAlert(guild, settings, RaidProtectionEmbedFactory.buildAlertEmbed(
                    "Anti-Bot: Bot entfernt", user, config.botKickReason(), RaidProtectionAction.KICK));
            return;
        }

        boolean raidJustTriggered = service.registerJoinAndCheckRaidTriggered(guildId);
        if (raidJustTriggered && config.alertEnabled()) {
            sendAlert(guild, settings, RaidProtectionEmbedFactory.buildRaidModeEmbed(
                    config.raidModeDurationSeconds(), config.joinThreshold()));
        }

        if (service.isRaidModeActive(guildId)) {
            executeAction(guild, member, config.raidModeAction(), config.raidModeReason());
            sendAlert(guild, settings, RaidProtectionEmbedFactory.buildAlertEmbed(
                    "Anti-Raid: Mitglied entfernt", user, config.raidModeReason(), config.raidModeAction()));
            return;
        }

        if (config.accountMinAgeMinutes() > 0) {
            long accountAgeMinutes = Duration.between(user.getTimeCreated().toInstant(), Instant.now()).toMinutes();
            if (accountAgeMinutes < config.accountMinAgeMinutes()) {
                executeAction(guild, member, config.accountMinAgeAction(), config.accountMinAgeReason());
                sendAlert(guild, settings, RaidProtectionEmbedFactory.buildAlertEmbed(
                        "Anti-Raid: Account zu neu", user, config.accountMinAgeReason(), config.accountMinAgeAction()));
            }
        }
    }

    private void executeAction(Guild guild, Member member, RaidProtectionAction action, String reason) {
        switch (action) {
            case KICK -> guild.kick(member).reason(reason).queue(
                    unused -> logAndAudit(guild, member, "RAIDPROTECTION_KICK", reason),
                    failure -> LOGGER.warn("Could not kick {} in guild {}: {}", member.getId(), guild.getId(), failure.getMessage()));
            case BAN -> guild.ban(member, 0, java.util.concurrent.TimeUnit.SECONDS).reason(reason).queue(
                    unused -> logAndAudit(guild, member, "RAIDPROTECTION_BAN", reason),
                    failure -> LOGGER.warn("Could not ban {} in guild {}: {}", member.getId(), guild.getId(), failure.getMessage()));
        }
    }

    private void logAndAudit(Guild guild, Member member, String action, String reason) {
        LOGGER.info("Raid protection executed {} on {} in guild {}: {}", action, member.getId(), guild.getId(), reason);
        AuditLog.record(guild.getId(), "RAIDPROTECTION", action, "target=" + member.getId() + " reason=" + reason);
    }

    private void sendAlert(Guild guild, RaidProtectionSettings settings, net.dv8tion.jda.api.entities.MessageEmbed embed) {
        if (settings.alertChannelId() == null || settings.alertChannelId().isBlank()) {
            return;
        }
        TextChannel channel = guild.getJDA().getChannelById(TextChannel.class, settings.alertChannelId());
        if (channel == null) {
            return;
        }
        channel.sendMessageEmbeds(embed).queue(unused -> {
        }, failure -> LOGGER.warn("Could not send raid protection alert in guild {}: {}", guild.getId(), failure.getMessage()));
    }
}
