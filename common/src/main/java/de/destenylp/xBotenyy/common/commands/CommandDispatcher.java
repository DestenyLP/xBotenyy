package de.destenylp.xBotenyy.common.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Function;

public final class CommandDispatcher<C> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandDispatcher.class);

    private final CommandRegistry<C> registry;
    private final CooldownManager cooldownManager;
    private final Function<C, CommandPermission> permissionResolver;
    private final Function<C, String> cooldownKeyResolver;

    public CommandDispatcher(CommandRegistry<C> registry, CooldownManager cooldownManager,
                              Function<C, CommandPermission> permissionResolver,
                              Function<C, String> cooldownKeyResolver) {
        this.registry = registry;
        this.cooldownManager = cooldownManager;
        this.permissionResolver = permissionResolver;
        this.cooldownKeyResolver = cooldownKeyResolver;
    }

    public CommandRegistry<C> getRegistry() {
        return registry;
    }

    public CommandDispatchResult dispatch(String commandName, C context) {
        Optional<Command<C>> maybeCommand = registry.find(commandName);
        if (maybeCommand.isEmpty()) {
            return CommandDispatchResult.UNKNOWN_COMMAND;
        }
        Command<C> command = maybeCommand.get();

        CommandPermission actualPermission = permissionResolver.apply(context);
        if (!actualPermission.isAtLeast(command.getPermission())) {
            return CommandDispatchResult.NO_PERMISSION;
        }

        if (command.getCooldownSeconds() > 0) {
            String cooldownKey = command.getName() + ":" + cooldownKeyResolver.apply(context);
            if (!cooldownManager.tryAcquire(cooldownKey, command.getCooldownSeconds())) {
                return CommandDispatchResult.ON_COOLDOWN;
            }
        }

        try {
            command.execute(context);
            return CommandDispatchResult.EXECUTED;
        } catch (Exception e) {
            LOGGER.error("Fehler bei der Ausfuehrung des Befehls '{}': ", command.getName(), e);
            return CommandDispatchResult.ERROR;
        }
    }
}
