package de.destenylp.xBotenyy.twitchbot.commands;

import de.destenylp.xBotenyy.common.commands.Command;
import de.destenylp.xBotenyy.common.commands.CommandPermission;

import java.util.List;

public abstract class AbstractTwitchCommand implements Command<TwitchCommandContext> {
    private final String name;
    private final String description;
    private final List<String> aliases;
    private final CommandPermission permission;
    private final int cooldownSeconds;

    protected AbstractTwitchCommand(String name, String description) {
        this(name, description, List.of(), CommandPermission.EVERYONE, 5);
    }

    protected AbstractTwitchCommand(String name, String description, CommandPermission permission) {
        this(name, description, List.of(), permission, 5);
    }

    protected AbstractTwitchCommand(String name, String description, List<String> aliases,
                                     CommandPermission permission, int cooldownSeconds) {
        this.name = name;
        this.description = description;
        this.aliases = aliases;
        this.permission = permission;
        this.cooldownSeconds = cooldownSeconds;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final String getDescription() {
        return description;
    }

    @Override
    public final List<String> getAliases() {
        return aliases;
    }

    @Override
    public final CommandPermission getPermission() {
        return permission;
    }

    @Override
    public final int getCooldownSeconds() {
        return cooldownSeconds;
    }
}
