package de.destenylp.xBotenyy.discordbot.commands;

import de.destenylp.xBotenyy.common.commands.CommandDispatchResult;
import de.destenylp.xBotenyy.common.commands.CommandDispatcher;
import de.destenylp.xBotenyy.common.commands.CommandPermission;
import de.destenylp.xBotenyy.common.commands.CommandRegistry;
import de.destenylp.xBotenyy.common.commands.CooldownManager;
import de.destenylp.xBotenyy.common.util.AuditLog;
import de.destenylp.xBotenyy.discordbot.observability.BotMetrics;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.List;

public class DiscordCommandManager {
    private final CommandRegistry<SlashCommandInteractionEvent> registry = new CommandRegistry<>();
    private final CommandDispatcher<SlashCommandInteractionEvent> dispatcher = new CommandDispatcher<>(
            registry, new CooldownManager(),
            DiscordCommandManager::resolvePermission,
            event -> event.getUser().getId());

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
}
