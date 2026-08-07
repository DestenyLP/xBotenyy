package de.destenylp.xBotenyy.launcher.console.impl;

import de.destenylp.xBotenyy.launcher.bot.ManagedBot;
import de.destenylp.xBotenyy.launcher.console.BotTargetResolver;
import de.destenylp.xBotenyy.launcher.console.CommandContext;
import de.destenylp.xBotenyy.launcher.console.ConsoleCommand;

import java.util.List;

public final class StopCommand implements ConsoleCommand {
    static final long DEFAULT_TIMEOUT_SECONDS = 30;

    static long parseTimeout(String[] args) {
        if (args.length < 2) {
            return DEFAULT_TIMEOUT_SECONDS;
        }
        try {
            long value = Long.parseLong(args[1].trim());
            if (value <= 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("timeoutSeconds must be a positive integer, was: " + args[1]);
        }
    }

    @Override
    public String name() {
        return "stop";
    }

    @Override
    public List<String> aliases() {
        return List.of("kill");
    }

    @Override
    public String usage() {
        return "stop <discord|twitch|all> [timeoutSeconds]";
    }

    @Override
    public String description() {
        return "Stops the specified bot (or all) gracefully, without it restarting automatically.";
    }

    @Override
    public void execute(String[] args, CommandContext context) {
        List<ManagedBot> targets = BotTargetResolver.resolve(args, context.bots(), name());
        long timeoutSeconds = parseTimeout(args);
        for (ManagedBot bot : targets) {
            context.print("Stopping " + bot.getDisplayName() + " (timeout " + timeoutSeconds + "s) ...");
            boolean stopped = bot.stop(timeoutSeconds);
            if (stopped) {
                context.print(bot.getDisplayName() + " was stopped.");
            } else {
                context.print(bot.getDisplayName() + " did not respond in time, shutdown continues in the background.");
            }
        }
    }
}

