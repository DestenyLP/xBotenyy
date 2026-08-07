package de.destenylp.xBotenyy.launcher.console.impl;

import de.destenylp.xBotenyy.launcher.console.CommandContext;
import de.destenylp.xBotenyy.launcher.console.ConsoleCommand;
import de.destenylp.xBotenyy.launcher.scheduler.DurationParser;
import de.destenylp.xBotenyy.launcher.scheduler.ScheduledAction;
import de.destenylp.xBotenyy.launcher.scheduler.ScheduledTask;
import de.destenylp.xBotenyy.launcher.scheduler.TaskScheduler;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

public final class ScheduleCommand implements ConsoleCommand {
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DAILY_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public String name() {
        return "schedule";
    }

    @Override
    public List<String> aliases() {
        return List.of("sched", "cron");
    }

    @Override
    public String usage() {
        return "schedule <add|list|remove|enable|disable> ...";
    }

    @Override
    public String description() {
        return "Manages scheduled tasks (restart/stop/start) for the bots, e.g. daily restarts.";
    }

    @Override
    public void execute(String[] args, CommandContext context) {
        if (args.length == 0) {
            printUsage(context);
            return;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        String[] rest = args.length > 1 ? java.util.Arrays.copyOfRange(args, 1, args.length) : new String[0];
        switch (sub) {
            case "add" -> handleAdd(rest, context);
            case "list", "ls" -> handleList(context);
            case "remove", "rm", "delete" -> handleRemove(rest, context);
            case "enable" -> handleToggle(rest, context, true);
            case "disable" -> handleToggle(rest, context, false);
            default -> printUsage(context);
        }
    }

    private void printUsage(CommandContext context) {
        context.print("Usage:");
        context.print("  schedule add <discord|twitch|all> <restart|stop|start> interval <value>  [timeoutSeconds]");
        context.print("      value example: 30m, 6h, 1d");
        context.print("  schedule add <discord|twitch|all> <restart|stop|start> daily <HH:mm>     [timeoutSeconds]");
        context.print("  schedule list");
        context.print("  schedule remove <id>");
        context.print("  schedule enable <id>");
        context.print("  schedule disable <id>");
    }

    private void handleAdd(String[] args, CommandContext context) {
        if (args.length < 4) {
            throw new IllegalArgumentException("Usage: " + usage());
        }
        String target = args[0];
        ScheduledAction action = ScheduledAction.parse(args[1])
                .orElseThrow(() -> new IllegalArgumentException("Unknown action '" + args[1]
                        + "'. Valid values: restart, stop, start."));
        String scheduleType = args[2].toLowerCase(Locale.ROOT);
        String value = args[3];
        long timeoutSeconds = args.length >= 5 ? parseTimeout(args[4]) : 30;
        TaskScheduler scheduler = context.scheduler();
        ScheduledTask task;
        if (scheduleType.equals("interval") || scheduleType.equals("every")) {
            Duration interval = DurationParser.parse(value);
            task = scheduler.addInterval(target, action, interval, timeoutSeconds);
        } else if (scheduleType.equals("daily") || scheduleType.equals("at")) {
            LocalTime dailyTime = parseDailyTime(value);
            task = scheduler.addDaily(target, action, dailyTime, timeoutSeconds);
        } else {
            throw new IllegalArgumentException("Unknown schedule type '" + scheduleType
                    + "'. Valid values: interval, daily.");
        }
        context.print("Scheduled task created: " + task);
        context.print("Next run: " + TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(task.getNextRunMillis())));
    }

    private void handleList(CommandContext context) {
        List<ScheduledTask> tasks = context.scheduler().all();
        if (tasks.isEmpty()) {
            context.print("No scheduled tasks exist.");
            return;
        }
        context.print("Scheduled tasks:");
        for (ScheduledTask task : tasks) {
            String next = TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(task.getNextRunMillis()));
            String last = task.getLastRunMillis() < 0 ? "never"
                    : TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(task.getLastRunMillis()));
            context.printf("  %s  Target=%-8s Action=%-8s Next=%s Last=%s", task, task.getTarget(),
                    task.getAction(), next, last);
        }
    }

    private void handleRemove(String[] args, CommandContext context) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: schedule remove <id>");
        }
        boolean removed = context.scheduler().remove(args[0]);
        context.print(removed ? "Task " + args[0] + " removed." : "No task with ID '" + args[0] + "' found.");
    }

    private void handleToggle(String[] args, CommandContext context, boolean enabled) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: schedule " + (enabled ? "enable" : "disable") + " <id>");
        }
        boolean found = context.scheduler().setEnabled(args[0], enabled);
        context.print(found
                ? "Task " + args[0] + (enabled ? " enabled." : " disabled.")
                : "No task with ID '" + args[0] + "' found.");
    }

    private long parseTimeout(String raw) {
        try {
            long value = Long.parseLong(raw.trim());
            if (value <= 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("timeoutSeconds must be a positive integer, was: " + raw);
        }
    }

    private LocalTime parseDailyTime(String raw) {
        try {
            return LocalTime.parse(raw.trim(), DAILY_TIME_FORMAT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid time '" + raw + "'. Format: HH:mm, e.g. 04:30.");
        }
    }
}

