package de.destenylp.xBotenyy.launcher.console.impl;

import de.destenylp.xBotenyy.launcher.bot.ManagedBot;
import de.destenylp.xBotenyy.launcher.console.BotTargetResolver;
import de.destenylp.xBotenyy.launcher.console.CommandContext;
import de.destenylp.xBotenyy.launcher.console.ConsoleCommand;

import java.util.List;

public final class StartCommand implements ConsoleCommand {

    @Override
    public String name() {
        return "start";
    }

    @Override
    public List<String> aliases() {
        return List.of("run");
    }

    @Override
    public String usage() {
        return "start <discord|twitch|all>";
    }

    @Override
    public String description() {
        return "Starts the specified bot (or all) asynchronously if it is not already running.";
    }

    @Override
    public void execute(String[] args, CommandContext context) {
        List<ManagedBot> targets = BotTargetResolver.resolve(args, context.bots(), name());
        for (ManagedBot bot : targets) {
            context.print("Starting " + bot.getDisplayName() + " ...");
            bot.start();
        }
    }
}
