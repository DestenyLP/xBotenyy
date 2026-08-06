package de.destenylp.xBotenyy.launcher.console.impl;
import de.destenylp.xBotenyy.launcher.console.CommandContext;
import de.destenylp.xBotenyy.launcher.console.ConsoleCommand;
import java.util.List;
public final class ExitCommand implements ConsoleCommand {
    @Override
    public String name() {
        return "exit";
    }
    @Override
    public List<String> aliases() {
        return List.of("quit", "shutdown");
    }
    @Override
    public String usage() {
        return "exit";
    }
    @Override
    public String description() {
        return "Stops all running bots gracefully and terminates the launcher process.";
    }
    @Override
    public void execute(String[] args, CommandContext context) {
        context.print("Shutting down launcher, stopping all bots gracefully...");
        context.requestLauncherShutdown();
    }
}
