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
            throw new IllegalArgumentException("timeoutSekunden muss eine positive Ganzzahl sein, war: " + args[1]);
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
        return "stop <discord|twitch|all> [timeoutSekunden]";
    }

    @Override
    public String description() {
        return "Stoppt den angegebenen Bot (oder alle) geordnet, ohne dass er automatisch neustartet.";
    }

    @Override
    public void execute(String[] args, CommandContext context) {
        List<ManagedBot> targets = BotTargetResolver.resolve(args, context.bots(), name());
        long timeoutSeconds = parseTimeout(args);
        for (ManagedBot bot : targets) {
            context.print("Stoppe " + bot.getDisplayName() + " (Timeout " + timeoutSeconds + "s) ...");
            boolean stopped = bot.stop(timeoutSeconds);
            if (stopped) {
                context.print(bot.getDisplayName() + " wurde gestoppt.");
            } else {
                context.print(bot.getDisplayName() + " antwortet nicht rechtzeitig, Shutdown laeuft im Hintergrund weiter.");
            }
        }
    }
}
