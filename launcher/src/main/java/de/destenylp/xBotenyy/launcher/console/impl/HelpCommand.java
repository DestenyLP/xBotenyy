package de.destenylp.xBotenyy.launcher.console.impl;

import de.destenylp.xBotenyy.launcher.console.CommandContext;
import de.destenylp.xBotenyy.launcher.console.ConsoleCommand;

import java.util.List;

public final class HelpCommand implements ConsoleCommand {

    @Override
    public String name() {
        return "help";
    }

    @Override
    public List<String> aliases() {
        return List.of("?", "commands");
    }

    @Override
    public String usage() {
        return "help";
    }

    @Override
    public String description() {
        return "Shows this overview of all available commands.";
    }

    @Override
    public void execute(String[] args, CommandContext context) {
        context.print("Available commands:");
        int longestUsage = context.commands().all().stream()
                .mapToInt(command -> command.usage().length())
                .max().orElse(0);
        for (ConsoleCommand command : context.commands().all()) {
            context.print(String.format("  %-" + longestUsage + "s  - %s", command.usage(), command.description()));
        }
        context.print("Bot identifiers: discord (alias: dc, discordbot) | twitch (alias: tw, twitchbot) | all");
    }
}
