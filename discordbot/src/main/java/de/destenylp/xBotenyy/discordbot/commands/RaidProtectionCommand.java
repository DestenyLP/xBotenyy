package de.destenylp.xBotenyy.discordbot.commands;

import de.destenylp.xBotenyy.common.util.AuditLog;
import de.destenylp.xBotenyy.discordbot.core.AbstractGuildCommand;
import de.destenylp.xBotenyy.discordbot.raidprotection.RaidProtectionConfig;
import de.destenylp.xBotenyy.discordbot.raidprotection.RaidProtectionService;
import de.destenylp.xBotenyy.discordbot.raidprotection.RaidProtectionSettings;
import de.destenylp.xBotenyy.discordbot.util.DiscordColors;
import de.destenylp.xBotenyy.discordbot.util.PermissionGuard;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RaidProtectionCommand extends AbstractGuildCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(RaidProtectionCommand.class);
    private final RaidProtectionService service;

    public RaidProtectionCommand(RaidProtectionService service) {
        this.service = service;
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("raidprotection", "Verwalte den Raid- und Bot-Schutz dieses Servers")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER))
                .addSubcommands(
                        new SubcommandData("toggle", "Aktiviert oder deaktiviert den Raid- und Bot-Schutz")
                                .addOptions(new OptionData(OptionType.BOOLEAN, "enabled", "Aktivieren?", true)),
                        new SubcommandData("alertchannel", "Setzt (oder entfernt) den Alarm-Kanal")
                                .addOptions(new OptionData(OptionType.CHANNEL, "channel", "Alarm-Kanal (leer = deaktivieren)", false)
                                        .setChannelTypes(ChannelType.TEXT)),
                        new SubcommandData("status", "Zeigt die aktuelle Konfiguration")
                );
    }

    @Override
    protected void executeInGuild(SlashCommandInteractionEvent event, Guild guild, String subcommand) {
        if (!PermissionGuard.requireManageServer(event)) {
            return;
        }
        switch (subcommand) {
            case "toggle" -> handleToggle(event, guild);
            case "alertchannel" -> handleAlertChannel(event, guild);
            case "status" -> handleStatus(event, guild);
            default -> replyUnknownSubcommand(event);
        }
    }

    private void handleToggle(SlashCommandInteractionEvent event, Guild guild) {
        boolean enabled = event.getOption("enabled").getAsBoolean();
        service.setEnabled(guild.getId(), enabled);
        event.reply((enabled ? "\u2705" : "\u274C") + " Raid- und Bot-Schutz wurde "
                + (enabled ? "aktiviert" : "deaktiviert") + ".").setEphemeral(true).queue();
        LOGGER.info("Raid protection set to {} for guild {}", enabled, guild.getId());
        AuditLog.record(guild.getId(), event.getUser().getId(), "RAIDPROTECTION_TOGGLE", "enabled=" + enabled);
    }

    private void handleAlertChannel(SlashCommandInteractionEvent event, Guild guild) {
        OptionMapping channelOption = event.getOption("channel");
        String channelId = channelOption != null ? channelOption.getAsChannel().asTextChannel().getId() : null;
        service.setAlertChannel(guild.getId(), channelId);
        String message = channelId != null
                ? "Alarm-Kanal wurde auf <#" + channelId + "> gesetzt."
                : "Alarm-Kanal wurde entfernt.";
        event.reply(message).setEphemeral(true).queue();
        LOGGER.info("Raid protection alert channel set to {} for guild {}", channelId, guild.getId());
        AuditLog.record(guild.getId(), event.getUser().getId(), "RAIDPROTECTION_ALERTCHANNEL", "channel=" + channelId);
    }

    private void handleStatus(SlashCommandInteractionEvent event, Guild guild) {
        RaidProtectionSettings settings = service.getSettings(guild.getId());
        RaidProtectionConfig config = service.getConfig();
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(DiscordColors.brand());
        eb.setTitle("\uD83D\uDEE1\uFE0F Raid-Schutz \u2013 " + guild.getName());
        eb.addField("Status", settings.enabled() ? "\u2705 Aktiv" : "\u274C Inaktiv", true);
        eb.addField("Raid-Modus", service.isRaidModeActive(guild.getId())
                ? "\uD83D\uDEA8 Aktiv (" + service.getRaidModeRemainingSeconds(guild.getId()) + "s verbleibend)"
                : "Inaktiv", true);
        eb.addField("Alarm-Kanal", settings.alertChannelId() != null ? "<#" + settings.alertChannelId() + ">" : "Nicht gesetzt", true);
        eb.addField("Beitritts-Schwelle", config.joinThreshold() + " Beitritte / " + config.joinWindowSeconds() + "s", true);
        eb.addField("Raid-Modus Dauer", config.raidModeDurationSeconds() + "s", true);
        eb.addField("Raid-Modus Aktion", config.raidModeAction().name(), true);
        eb.addField("Mindest-Account-Alter", config.accountMinAgeMinutes() + " Minuten", true);
        eb.addField("Account-Alter Aktion", config.accountMinAgeAction().name(), true);
        eb.addField("Bot-Autokick", config.botAutoKickEnabled() ? "\u2705 Aktiv" : "\u274C Inaktiv", true);
        event.replyEmbeds(eb.build()).setEphemeral(true).queue();
    }
}
