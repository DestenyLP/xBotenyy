package de.destenylp.xBotenyy.launcher.console;

import java.util.List;

public interface ConsoleCommand {

    String name();

    default List<String> aliases() {
        return List.of();
    }

    String usage();

    String description();

    void execute(String[] args, CommandContext context);
}
