package de.destenylp.xBotenyy.launcher;

import de.destenylp.xBotenyy.launcher.bot.BotRegistry;
import de.destenylp.xBotenyy.launcher.bot.DiscordManagedBot;
import de.destenylp.xBotenyy.launcher.bot.ManagedBot;
import de.destenylp.xBotenyy.launcher.bot.TwitchManagedBot;
import de.destenylp.xBotenyy.launcher.console.CommandContext;
import de.destenylp.xBotenyy.launcher.console.ConsoleCommandRegistry;
import de.destenylp.xBotenyy.launcher.console.ConsoleShell;
import de.destenylp.xBotenyy.launcher.console.impl.*;
import de.destenylp.xBotenyy.launcher.scheduler.SchedulerStore;
import de.destenylp.xBotenyy.launcher.scheduler.TaskScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

public final class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final long SHUTDOWN_STOP_TIMEOUT_SECONDS = 30;

    private Main() {
    }

    public static void main(String[] args) throws InterruptedException {
        LaunchMode mode = LaunchMode.fromArgs(args);
        LauncherSettings settings = LauncherSettings.loadFromEnvironment();
        LOGGER.info("xBotenyy Launcher starting in mode {} ({})", mode, settings);
        BotRegistry registry = new BotRegistry();
        if (mode.includesDiscord()) {
            registry.register(new DiscordManagedBot(LOGGER, settings));
        }
        if (mode.includesTwitch()) {
            registry.register(new TwitchManagedBot(LOGGER, settings));
        }
        if (registry.isEmpty()) {
            LOGGER.error("Mode {} does not enable any bot, launcher is shutting down.", mode);
            return;
        }
        TaskScheduler scheduler = new TaskScheduler(registry, new SchedulerStore(LOGGER), LOGGER);
        CountDownLatch shutdownLatch = new CountDownLatch(1);
        Runnable shutdownAction = () -> initiateLauncherShutdown(registry, scheduler, shutdownLatch);
        ConsoleCommandRegistry commandRegistry = buildCommandRegistry();
        CommandContext commandContext = new CommandContext(registry, settings, commandRegistry, shutdownAction,
                scheduler);
        ConsoleShell consoleShell = new ConsoleShell(commandRegistry, commandContext, LOGGER);
        Runtime.getRuntime().addShutdownHook(new Thread(
                () -> initiateLauncherShutdown(registry, scheduler, shutdownLatch), "launcher-shutdown-hook"));
        for (ManagedBot bot : registry.all()) {
            bot.start();
        }
        scheduler.start();
        consoleShell.start();
        shutdownLatch.await();
        LOGGER.info("xBotenyy Launcher has been shut down.");
    }

    private static ConsoleCommandRegistry buildCommandRegistry() {
        ConsoleCommandRegistry registry = new ConsoleCommandRegistry();
        registry.register(new HelpCommand());
        registry.register(new StatusCommand());
        registry.register(new StartCommand());
        registry.register(new StopCommand());
        registry.register(new RestartCommand());
        registry.register(new SetCommand());
        registry.register(new ScheduleCommand());
        registry.register(new ExitCommand());
        return registry;
    }

    private static synchronized void initiateLauncherShutdown(BotRegistry registry, TaskScheduler scheduler,
                                                              CountDownLatch shutdownLatch) {
        if (shutdownLatch.getCount() == 0) {
            return;
        }
        LOGGER.info("Launcher shutdown requested, stopping all bots gracefully...");
        scheduler.stop();
        for (ManagedBot bot : registry.all()) {
            bot.stop(SHUTDOWN_STOP_TIMEOUT_SECONDS);
        }
        shutdownLatch.countDown();
    }
}

