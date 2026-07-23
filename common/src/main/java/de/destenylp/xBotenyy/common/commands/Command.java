package de.destenylp.xBotenyy.common.commands;

import java.util.List;

public interface Command<C> {
    String getName();

    default List<String> getAliases() {
        return List.of();
    }

    String getDescription();

    default CommandPermission getPermission() {
        return CommandPermission.EVERYONE;
    }

    default int getCooldownSeconds() {
        return 0;
    }

    void execute(C context) throws Exception;
}
