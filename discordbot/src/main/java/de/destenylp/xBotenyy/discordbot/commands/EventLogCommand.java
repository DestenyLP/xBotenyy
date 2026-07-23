package de.destenylp.xBotenyy.discordbot.commands;

import de.destenylp.xBotenyy.discordbot.core.AbstractGuildCommand;
import de.destenylp.xBotenyy.discordbot.eventlog.EventLogRule;
import de.destenylp.xBotenyy.discordbot.eventlog.EventLogService;
import de.destenylp.xBotenyy.discordbot.eventlog.EventLogSettings;
import de.destenylp.xBotenyy.discordbot.eventlog.LogEventType;
import de.destenylp.xBotenyy.common.util.AuditLog;
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

import java.util.Optional;

public class EventLogCommand extends AbstractGuildCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventLogCommand.class);

    private final EventLogService service;

    public EventLogCommand(EventLogService service) {
        this.service = service;
    }

    @Override
    public CommandData getCommandData() {
        OptionData eventOption = new OptionData(OptionType.STRING, "event", "Event-Typ", true);
        for (LogEventType type : LogEventType.values()) {
            eventOption.addChoice(type.getLabel(), type.getKey());
        }

        return Commands.slash("serverlog", "Verwalte das Event-Logging (Joins, Boosts, uvm.)")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER))
                .addSubcommands(
                        new SubcommandData("setup", "Richtet das Logging mit einem Standardkanal ein und aktiviert alle Events")
                                .addOptions(new OptionData(OptionType.CHANNEL, "channel", "Standard-Log-Kanal", true)
                                        .setChannelTypes(ChannelType.TEXT)),
                        new SubcommandData("channel", "Ändert nur den Standard-Log-Kanal")
                                .addOptions(new OptionData(OptionType.CHANNEL, "channel", "Standard-Log-Kanal", true)
                                        .setChannelTypes(ChannelType.TEXT)),
                        new SubcommandData("toggle", "Aktiviert/deaktiviert einen einzelnen Event-Typ")
                                .addOptions(eventOption)
                                .addOption(OptionType.BOOLEAN, "enabled", "Aktivieren?", true),
                        new SubcommandData("event-channel", "Setzt (oder entfernt) einen abweichenden Kanal für einen Event-Typ")
                                .addOptions(new OptionData(OptionType.STRING, "event", "Event-Typ", true).addChoices(eventOption.getChoices()))
                                .addOptions(new OptionData(OptionType.CHANNEL, "channel", "Abweichender Kanal (leer = Standardkanal nutzen)", false)
                                        .setChannelTypes(ChannelType.TEXT)),
                        new SubcommandData("status", "Zeigt die aktuelle Logging-Konfiguration")
                );
    }

    @Override
    protected void executeInGuild(SlashCommandInteractionEvent event, Guild guild, String subcommand) {
        if (!PermissionGuard.requireManageServer(event)) {
            return;
        }

        switch (subcommand) {
            case "setup" -> handleSetup(event, guild);
            case "channel" -> handleChannel(event, guild);
            case "toggle" -> handleToggle(event, guild);
            case "event-channel" -> handleEventChannel(event, guild);
            case "status" -> handleStatus(event, guild);
            default -> replyUnknownSubcommand(event);
        }
    }

    private void handleSetup(SlashCommandInteractionEvent event, Guild guild) {
        TextChannel channel = event.getOption("channel").getAsChannel().asTextChannel();
        service.setup(guild.getId(), channel.getId());
        event.reply("Event-Logging eingerichtet! Standardkanal: " + channel.getAsMention() +
                        "\nAlle Event-Typen sind jetzt aktiv. Mit `/serverlog toggle` kannst du einzelne deaktivieren.")
                .setEphemeral(true).queue();
        LOGGER.info("Event log setup for guild {} with channel {}", guild.getId(), channel.getId());
        AuditLog.record(guild.getId(), event.getUser().getId(), "EVENTLOG_SETUP", "channel=" + channel.getId());
    }

    private void handleChannel(SlashCommandInteractionEvent event, Guild guild) {
        TextChannel channel = event.getOption("channel").getAsChannel().asTextChannel();
        service.setDefaultChannel(guild.getId(), channel.getId());
        event.reply("Standard-Log-Kanal wurde auf " + channel.getAsMention() + " gesetzt.").setEphemeral(true).queue();
        LOGGER.info("Event log default channel updated for guild {}", guild.getId());
        AuditLog.record(guild.getId(), event.getUser().getId(), "EVENTLOG_CHANNEL_UPDATE", "channel=" + channel.getId());
    }

    private void handleToggle(SlashCommandInteractionEvent event, Guild guild) {
        Optional<LogEventType> typeOpt = LogEventType.fromKey(event.getOption("event").getAsString());
        if (typeOpt.isEmpty()) {
            event.reply("Unbekannter Event-Typ.").setEphemeral(true).queue();
            return;
        }
        boolean enabled = event.getOption("enabled").getAsBoolean();
        LogEventType type = typeOpt.get();
        service.setEventEnabled(guild.getId(), type, enabled);
        event.reply(type.getEmoji() + " **" + type.getLabel() + "** wurde " + (enabled ? "aktiviert" : "deaktiviert") + ".")
                .setEphemeral(true).queue();
        LOGGER.info("Event log type {} set to {} for guild {}", type, enabled, guild.getId());
        AuditLog.record(guild.getId(), event.getUser().getId(), "EVENTLOG_TOGGLE", "type=" + type.getKey() + " enabled=" + enabled);
    }

    private void handleEventChannel(SlashCommandInteractionEvent event, Guild guild) {
        Optional<LogEventType> typeOpt = LogEventType.fromKey(event.getOption("event").getAsString());
        if (typeOpt.isEmpty()) {
            event.reply("Unbekannter Event-Typ.").setEphemeral(true).queue();
            return;
        }
        LogEventType type = typeOpt.get();
        OptionMapping channelOption = event.getOption("channel");
        String channelId = channelOption != null ? channelOption.getAsChannel().asTextChannel().getId() : null;
        service.setEventChannel(guild.getId(), type, channelId);

        String message = channelId != null
                ? type.getEmoji() + " **" + type.getLabel() + "** wird jetzt in <#" + channelId + "> geloggt."
                : type.getEmoji() + " **" + type.getLabel() + "** nutzt jetzt wieder den Standardkanal.";
        event.reply(message).setEphemeral(true).queue();
        LOGGER.info("Event log channel override for {} set to {} in guild {}", type, channelId, guild.getId());
        AuditLog.record(guild.getId(), event.getUser().getId(), "EVENTLOG_EVENT_CHANNEL", "type=" + type.getKey() + " channel=" + channelId);
    }

    private void handleStatus(SlashCommandInteractionEvent event, Guild guild) {
        EventLogSettings settings = service.getSettings(guild.getId()).orElse(null);

        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(DiscordColors.brand());
        eb.setTitle("\uD83D\uDCCB Event-Logging – " + guild.getName());

        if (settings == null || settings.getDefaultChannelId() == null) {
            eb.setDescription("Noch nicht eingerichtet. Nutze `/serverlog setup channel:#kanal`.");
            event.replyEmbeds(eb.build()).setEphemeral(true).queue();
            return;
        }

        eb.setDescription("Standardkanal: <#" + settings.getDefaultChannelId() + ">");
        for (LogEventType type : LogEventType.values()) {
            boolean enabled = service.isEnabled(settings, type);
            EventLogRule rule = settings.getRules().get(type);
            String channelInfo = rule != null && rule.getChannelId() != null
                    ? "<#" + rule.getChannelId() + ">"
                    : "Standardkanal";
            eb.addField(type.getEmoji() + " " + type.getLabel(),
                    (enabled ? "\u2705 Aktiv" : "\u274C Inaktiv") + " · " + channelInfo, true);
        }

        event.replyEmbeds(eb.build()).setEphemeral(true).queue();
    }
}
