package de.destenylp.xBotenyy.twitchbot.commands;

import de.destenylp.xBotenyy.common.commands.CommandDispatchResult;
import de.destenylp.xBotenyy.common.commands.CommandDispatcher;
import de.destenylp.xBotenyy.common.commands.CommandPermission;
import de.destenylp.xBotenyy.common.commands.CommandRegistry;
import de.destenylp.xBotenyy.common.commands.CooldownManager;
import de.destenylp.xBotenyy.common.observability.Metrics;
import de.destenylp.xBotenyy.twitchbot.chat.TwitchChatMessage;
import de.destenylp.xBotenyy.twitchbot.eventlog.TwitchEventLogService;
import de.destenylp.xBotenyy.twitchbot.persistence.CustomCommandRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class TwitchCommandManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TwitchCommandManager.class);

    private final String prefix;
    private final CommandRegistry<TwitchCommandContext> registry = new CommandRegistry<>();
    private final CommandDispatcher<TwitchCommandContext> dispatcher;
    private final CustomCommandRepository customCommandRepository;
    private final TwitchBotServices services;
    private final TwitchEventLogService eventLogService;

    public TwitchCommandManager(String prefix, CustomCommandRepository customCommandRepository,
                                 TwitchBotServices services, TwitchEventLogService eventLogService) {
        this.prefix = prefix;
        this.customCommandRepository = customCommandRepository;
        this.services = services;
        this.eventLogService = eventLogService;
        this.dispatcher = new CommandDispatcher<>(registry, new CooldownManager(),
                context -> resolvePermission(context.message()),
                context -> context.message().userId());
    }

    public CommandRegistry<TwitchCommandContext> getRegistry() {
        return registry;
    }

    public void register(de.destenylp.xBotenyy.common.commands.Command<TwitchCommandContext> command) {
        registry.register(command);
    }

    public boolean handleMessage(TwitchChatMessage message) {
        if (!message.content().startsWith(prefix)) {
            return false;
        }
        String withoutPrefix = message.content().substring(prefix.length()).trim();
        if (withoutPrefix.isEmpty()) {
            return false;
        }

        List<String> parts = List.of(withoutPrefix.split("\\s+"));
        String commandName = parts.get(0).toLowerCase(java.util.Locale.ROOT);
        List<String> args = parts.size() > 1 ? parts.subList(1, parts.size()) : List.of();

        TwitchCommandContext context = new TwitchCommandContext(message, args, services);

        if (registry.find(commandName).isPresent()) {
            dispatchBuiltIn(commandName, context);
            return true;
        }

        return handleCustomCommand(message, commandName);
    }

    private void dispatchBuiltIn(String commandName, TwitchCommandContext context) {
        Metrics.increment("twitch.commands_executed");
        CommandDispatchResult result = dispatcher.dispatch(commandName, context);
        switch (result) {
            case NO_PERMISSION -> LOGGER.debug("Befehl '{}' von {} in {} abgelehnt: fehlende Berechtigung.",
                    commandName, context.message().userLogin(), context.message().channelLogin());
            case ON_COOLDOWN -> LOGGER.debug("Befehl '{}' von {} in {} ist im Cooldown.",
                    commandName, context.message().userLogin(), context.message().channelLogin());
            case ERROR -> eventLogService.record(context.message().channelLogin(), context.message().userId(),
                    "COMMAND_ERROR", "command=" + commandName);
            default -> {
            }
        }
    }

    private boolean handleCustomCommand(TwitchChatMessage message, String commandName) {
        Optional<CustomCommandRepository.CustomCommandRecord> custom =
                customCommandRepository.find(message.channelLogin(), commandName);
        if (custom.isEmpty()) {
            return false;
        }
        String response = custom.get().response()
                .replace("{user}", message.displayName())
                .replace("{channel}", message.channelLogin());
        services.reply(message.channelLogin(), response);
        customCommandRepository.incrementUses(message.channelLogin(), commandName);
        Metrics.increment("twitch.custom_commands_executed");
        return true;
    }

    private static CommandPermission resolvePermission(TwitchChatMessage message) {
        if (message.broadcaster()) {
            return CommandPermission.BROADCASTER;
        }
        if (message.moderator()) {
            return CommandPermission.MODERATOR;
        }
        if (message.vip()) {
            return CommandPermission.VIP;
        }
        if (message.subscriber()) {
            return CommandPermission.SUBSCRIBER;
        }
        return CommandPermission.EVERYONE;
    }
}
