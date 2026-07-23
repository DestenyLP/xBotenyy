package de.destenylp.xBotenyy.twitchbot.commands.impl;

import de.destenylp.xBotenyy.common.commands.CommandPermission;
import de.destenylp.xBotenyy.twitchbot.commands.AbstractTwitchCommand;
import de.destenylp.xBotenyy.twitchbot.commands.TwitchCommandContext;
import de.destenylp.xBotenyy.twitchbot.eventlog.TwitchEventLogService;
import de.destenylp.xBotenyy.twitchbot.persistence.CustomCommandRepository;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class CustomCommandManagementCommand extends AbstractTwitchCommand {
    private static final List<String> RESERVED_NAMES =
            List.of("ping", "uptime", "automod", "mod", "strikes", "commands", "help", "command");

    private final CustomCommandRepository repository;
    private final TwitchEventLogService eventLogService;

    public CustomCommandManagementCommand(CustomCommandRepository repository, TwitchEventLogService eventLogService) {
        super("command", "Verwaltet Custom-Commands (add/remove/list).", List.of("cmd"),
                CommandPermission.MODERATOR, 2);
        this.repository = repository;
        this.eventLogService = eventLogService;
    }

    @Override
    public void execute(TwitchCommandContext context) {
        String channel = context.message().channelLogin();
        String sub = context.arg(0) == null ? "" : context.arg(0).toLowerCase(Locale.ROOT);

        switch (sub) {
            case "add" -> handleAdd(context, channel);
            case "remove", "delete" -> handleRemove(context, channel);
            case "list" -> handleList(context, channel);
            default -> context.reply("Nutzung: " + "!command add <name> <antwort> | !command remove <name> | !command list");
        }
    }

    private void handleAdd(TwitchCommandContext context, String channel) {
        if (context.args().size() < 3) {
            context.reply("Nutzung: !command add <name> <antwort>");
            return;
        }
        String name = context.arg(1).toLowerCase(Locale.ROOT).replace("!", "");
        if (RESERVED_NAMES.contains(name)) {
            context.reply("'" + name + "' ist ein eingebauter Befehl und kann nicht ueberschrieben werden.");
            return;
        }
        String response = String.join(" ", context.args().subList(2, context.args().size()));
        repository.upsert(channel, name, response, context.message().userLogin());
        eventLogService.record(channel, context.message().userId(), "CUSTOM_COMMAND_ADDED", "name=" + name);
        context.reply("Befehl '" + name + "' wurde gespeichert.");
    }

    private void handleRemove(TwitchCommandContext context, String channel) {
        if (context.arg(1) == null) {
            context.reply("Nutzung: !command remove <name>");
            return;
        }
        String name = context.arg(1).toLowerCase(Locale.ROOT).replace("!", "");
        boolean removed = repository.remove(channel, name);
        if (removed) {
            eventLogService.record(channel, context.message().userId(), "CUSTOM_COMMAND_REMOVED", "name=" + name);
            context.reply("Befehl '" + name + "' wurde entfernt.");
        } else {
            context.reply("Es gibt keinen Custom-Command mit dem Namen '" + name + "'.");
        }
    }

    private void handleList(TwitchCommandContext context, String channel) {
        List<CustomCommandRepository.CustomCommandRecord> commands = repository.list(channel);
        if (commands.isEmpty()) {
            context.reply("Es sind noch keine Custom-Commands fuer diesen Kanal angelegt.");
            return;
        }
        String joined = commands.stream()
                .map(CustomCommandRepository.CustomCommandRecord::name)
                .collect(Collectors.joining(", "));
        context.reply("Custom-Commands: " + joined);
    }
}
