package de.destenylp.xBotenyy.launcher.console.impl;

import de.destenylp.xBotenyy.launcher.bot.ManagedBot;
import de.destenylp.xBotenyy.launcher.console.BotTargetResolver;
import de.destenylp.xBotenyy.launcher.console.CommandContext;
import de.destenylp.xBotenyy.launcher.console.ConsoleCommand;

import java.util.List;

public final class RestartCommand implements ConsoleCommand {

    @Override
    public String name() {
        return "restart";
    }

    @Override
    public List<String> aliases() {
        return List.of("reboot");
    }

    @Override
    public String usage() {
        return "restart <discord|twitch|all> [timeoutSekunden]";
    }

    @Override
    public String description() {
        return "Stoppt den angegebenen Bot (oder alle) geordnet und startet ihn danach sofort wieder.";
    }

    @Override
    public void execute(String[] args, CommandContext context) {
        List<ManagedBot> targets = BotTargetResolver.resolve(args, context.bots(), name());
        long timeoutSeconds = StopCommand.parseTimeout(args);
        for (ManagedBot bot : targets) {
            context.print("Starte " + bot.getDisplayName() + " neu (Stop-Timeout " + timeoutSeconds + "s) ...");
            bot.stop(timeoutSeconds);
            bot.start();
        }
    }
}
