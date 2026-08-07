package de.destenylp.xBotenyy.launcher.console.impl;

import de.destenylp.xBotenyy.launcher.bot.ManagedBot;
import de.destenylp.xBotenyy.launcher.console.CommandContext;
import de.destenylp.xBotenyy.launcher.console.ConsoleCommand;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class StatusCommand implements ConsoleCommand {
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    @Override
    public String name() {
        return "status";
    }

    @Override
    public List<String> aliases() {
        return List.of("info", "list");
    }

    @Override
    public String usage() {
        return "status";
    }

    @Override
    public String description() {
        return "Shows the current status of all bots as well as the restart settings.";
    }

    @Override
    public void execute(String[] args, CommandContext context) {
        if (context.bots().isEmpty()) {
            context.print("No bots registered.");
            return;
        }
        context.print("Status:");
        for (ManagedBot bot : context.bots().all()) {
            String lastStarted = bot.getLastStartedAtMillis() < 0
                    ? "never"
                    : TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(bot.getLastStartedAtMillis()));
            context.printf("  [%s] %-11s Status=%-8s Restarts=%-3d Last started=%s",
                    bot.getId().primaryName(), bot.getDisplayName(), bot.getStatus(), bot.getRestartCount(), lastStarted);
        }
        context.print("Settings: " + context.settings());
    }
}

