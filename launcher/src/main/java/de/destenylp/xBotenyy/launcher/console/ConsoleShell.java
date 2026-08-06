package de.destenylp.xBotenyy.launcher.console;
import org.slf4j.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
public final class ConsoleShell {
    private final ConsoleCommandRegistry commands;
    private final CommandContext context;
    private final Logger logger;
    private volatile boolean running = true;
    public ConsoleShell(ConsoleCommandRegistry commands, CommandContext context, Logger logger) {
        this.commands = commands;
        this.context = context;
        this.logger = logger;
    }
    public void start() {
        Thread thread = new Thread(this::loop, "console-shell");
        thread.setDaemon(true);
        thread.start();
    }
    public void stop() {
        running = false;
    }
    private void loop() {
        context.print("");
        context.print("xBotenyy Launcher console ready. Type 'help' for a list of all commands.");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while (running && (line = reader.readLine()) != null) {
                handleLine(line);
            }
        } catch (IOException e) {
            logger.error("Console input could not be read, the console is now disabled: ", e);
        }
    }
    private void handleLine(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        String[] tokens = trimmed.split("\\s+");
        String commandName = tokens[0];
        String[] args = Arrays.copyOfRange(tokens, 1, tokens.length);
        Optional<ConsoleCommand> command = commands.find(commandName);
        if (command.isEmpty()) {
            context.print("Unknown command: '" + commandName + "'. Type 'help' for an overview.");
            return;
        }
        try {
            command.get().execute(args, context);
        } catch (IllegalArgumentException e) {
            context.print("Error: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error while executing command '{}': ", commandName, e);
            context.print("Unexpected error while executing '" + commandName + "', see log for details.");
        }
    }
}
