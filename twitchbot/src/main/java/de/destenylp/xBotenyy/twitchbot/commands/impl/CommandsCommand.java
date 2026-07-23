package de.destenylp.xBotenyy.twitchbot.commands.impl;

import de.destenylp.xBotenyy.common.commands.Command;
import de.destenylp.xBotenyy.twitchbot.commands.AbstractTwitchCommand;
import de.destenylp.xBotenyy.twitchbot.commands.TwitchCommandContext;
import de.destenylp.xBotenyy.twitchbot.commands.TwitchCommandManager;
import de.destenylp.xBotenyy.twitchbot.persistence.CustomCommandRepository;

import java.util.List;
import java.util.stream.Collectors;

public class CommandsCommand extends AbstractTwitchCommand {
    private final TwitchCommandManager commandManager;
    private final CustomCommandRepository customCommandRepository;
    private final String prefix;

    public CommandsCommand(TwitchCommandManager commandManager, CustomCommandRepository customCommandRepository,
                            String prefix) {
        super("commands", "Listet alle verfuegbaren Befehle auf.", List.of("help"),
                de.destenylp.xBotenyy.common.commands.CommandPermission.EVERYONE, 15);
        this.commandManager = commandManager;
        this.customCommandRepository = customCommandRepository;
        this.prefix = prefix;
    }

    @Override
    public void execute(TwitchCommandContext context) {
        String builtIn = commandManager.getRegistry().all().stream()
                .map(Command::getName)
                .sorted()
                .map(name -> prefix + name)
                .collect(Collectors.joining(", "));

        List<CustomCommandRepository.CustomCommandRecord> custom =
                customCommandRepository.list(context.message().channelLogin());
        String customPart = custom.isEmpty() ? "" : " | Custom: " + custom.stream()
                .map(record -> prefix + record.name())
                .collect(Collectors.joining(", "));

        context.reply("Befehle: " + builtIn + customPart);
    }
}
