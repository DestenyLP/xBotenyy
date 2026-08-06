package de.destenylp.xBotenyy.discordbot.commands;
import de.destenylp.xBotenyy.common.commands.*;
import de.destenylp.xBotenyy.common.util.AuditLog;
import de.destenylp.xBotenyy.discordbot.eventlog.EventLogEmbedFactory;
import de.destenylp.xBotenyy.discordbot.eventlog.EventLogService;
import de.destenylp.xBotenyy.discordbot.eventlog.LogEventType;
import de.destenylp.xBotenyy.discordbot.observability.BotMetrics;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Optional;
public class DiscordCommandManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordCommandManager.class);
    private final CommandRegistry<SlashCommandInteractionEvent> registry = new CommandRegistry<>();
    private final CommandDispatcher<SlashCommandInteractionEvent> dispatcher = new CommandDispatcher<>(
            registry, new CooldownManager(),
            DiscordCommandManager::resolvePermission,
            event -> event.getUser().getId());
    private final EventLogService eventLogService;
    public DiscordCommandManager(EventLogService eventLogService) {
        this.eventLogService = eventLogService;
    }
    private static void replyError(SlashCommandInteractionEvent event) {
        String message = "Es ist ein unerwarteter Fehler aufgetreten. Bitte versuche es erneut.";
        if (event.isAcknowledged()) {
            event.getHook().sendMessage(message).queue();
        } else {
            event.reply(message).setEphemeral(true).queue();
        }
    }
    private static CommandPermission resolvePermission(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        if (member == null) {
            return CommandPermission.EVERYONE;
        }
        if (member.hasPermission(Permission.ADMINISTRATOR)) {
            return CommandPermission.ADMIN;
        }
        if (member.hasPermission(Permission.MANAGE_SERVER)) {
            return CommandPermission.MODERATOR;
        }
        return CommandPermission.EVERYONE;
    }
    public void register(DiscordCommand command) {
        registry.register(command);
    }
    public int size() {
        return registry.size();
    }
    public List<CommandData> allCommandData() {
        return registry.all().stream()
                .map(command -> ((DiscordCommand) command).getCommandData())
                .toList();
    }
    public void handle(SlashCommandInteractionEvent event) {
        CommandDispatchResult result = dispatcher.dispatch(event.getName(), event);
        String guildId = event.getGuild() != null ? event.getGuild().getId() : "DM";
        switch (result) {
            case EXECUTED -> BotMetrics.incrementCommandsExecuted();
            case UNKNOWN_COMMAND -> event.reply("Command not found!").setEphemeral(true).queue();
            case NO_PERMISSION -> event.reply("Dir fehlt die Berechtigung fuer diesen Befehl.")
                    .setEphemeral(true).queue();
            case ON_COOLDOWN -> event.reply("Bitte warte kurz, bevor du diesen Befehl erneut nutzt.")
                    .setEphemeral(true).queue();
            case ERROR -> {
                AuditLog.record(guildId, event.getUser().getId(), "COMMAND_ERROR", "command=" + event.getName());
                replyError(event);
            }
        }
        if (result != CommandDispatchResult.UNKNOWN_COMMAND) {
            logCommandUsage(event, result);
        }
    }
    private void logCommandUsage(SlashCommandInteractionEvent event, CommandDispatchResult result) {
        if (event.getGuild() == null) {
            return;
        }
        try {
            Optional<String> channelIdOpt = eventLogService.resolveChannelId(event.getGuild().getId(), LogEventType.COMMAND_USAGE);
            if (channelIdOpt.isEmpty()) {
                return;
            }
            TextChannel channel = event.getJDA().getChannelById(TextChannel.class, channelIdOpt.get());
            if (channel == null) {
                return;
            }
            channel.sendMessageEmbeds(EventLogEmbedFactory.buildCommandUsage(event, result))
                    .queue(success -> {
                    }, failure -> LOGGER.warn("Command usage log could not be sent: {}", failure.getMessage()));
        } catch (Exception e) {
            LOGGER.error("Unexpected error during command usage logging for guild {}: ", event.getGuild().getId(), e);
        }
    }
}
