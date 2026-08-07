package de.destenylp.xBotenyy.launcher.console;

import de.destenylp.xBotenyy.launcher.LauncherSettings;
import de.destenylp.xBotenyy.launcher.bot.BotRegistry;
import de.destenylp.xBotenyy.launcher.scheduler.TaskScheduler;

public final class CommandContext {
    private final BotRegistry bots;
    private final LauncherSettings settings;
    private final ConsoleCommandRegistry commands;
    private final Runnable shutdownAction;
    private final TaskScheduler scheduler;

    public CommandContext(BotRegistry bots, LauncherSettings settings, ConsoleCommandRegistry commands,
                          Runnable shutdownAction, TaskScheduler scheduler) {
        this.bots = bots;
        this.settings = settings;
        this.commands = commands;
        this.shutdownAction = shutdownAction;
        this.scheduler = scheduler;
    }

    public TaskScheduler scheduler() {
        return scheduler;
    }

    public BotRegistry bots() {
        return bots;
    }

    public LauncherSettings settings() {
        return settings;
    }

    public ConsoleCommandRegistry commands() {
        return commands;
    }

    public void requestLauncherShutdown() {
        shutdownAction.run();
    }

    public void print(String message) {
        System.out.println(message);
    }

    public void printf(String format, Object... args) {
        System.out.printf((format.endsWith("%n") ? format : format + "%n"), args);
    }
}

