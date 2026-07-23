package de.destenylp.xBotenyy.twitchbot.commands.impl;

import de.destenylp.xBotenyy.common.commands.CommandPermission;
import de.destenylp.xBotenyy.twitchbot.broadcast.TwitchBroadcastMessage;
import de.destenylp.xBotenyy.twitchbot.commands.AbstractTwitchCommand;
import de.destenylp.xBotenyy.twitchbot.commands.TwitchCommandContext;
import de.destenylp.xBotenyy.twitchbot.eventlog.TwitchEventLogService;
import de.destenylp.xBotenyy.twitchbot.persistence.TwitchBroadcastRepository;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class BroadcastCommand extends AbstractTwitchCommand {
    private static final String USAGE = "Nutzung: !broadcast add <intervall_sek> <min_nachrichten> <text> | "
            + "!broadcast remove <id> | !broadcast enable <id> | !broadcast disable <id> | !broadcast list";

    private final TwitchBroadcastRepository repository;
    private final TwitchEventLogService eventLogService;
    private final long defaultIntervalSeconds;
    private final int defaultMinMessages;

    public BroadcastCommand(TwitchBroadcastRepository repository, TwitchEventLogService eventLogService,
                             long defaultIntervalSeconds, int defaultMinMessages) {
        super("broadcast", "Verwaltet wiederkehrende Chat-Ansagen.", List.of("ansage"),
                CommandPermission.MODERATOR, 2);
        this.repository = repository;
        this.eventLogService = eventLogService;
        this.defaultIntervalSeconds = defaultIntervalSeconds;
        this.defaultMinMessages = defaultMinMessages;
    }

    @Override
    public void execute(TwitchCommandContext context) {
        String channel = context.message().channelLogin();
        String sub = context.arg(0) == null ? "" : context.arg(0).toLowerCase(Locale.ROOT);

        switch (sub) {
            case "add" -> handleAdd(context, channel);
            case "remove", "delete" -> handleRemove(context, channel);
            case "enable" -> handleToggle(context, channel, true);
            case "disable" -> handleToggle(context, channel, false);
            case "list" -> handleList(context, channel);
            default -> context.reply(USAGE);
        }
    }

    private void handleAdd(TwitchCommandContext context, String channel) {
        if (context.args().size() < 4) {
            context.reply(USAGE);
            return;
        }
        long intervalSeconds = parseLong(context.arg(1), defaultIntervalSeconds);
        int minMessages = (int) parseLong(context.arg(2), defaultMinMessages);
        String text = String.join(" ", context.args().subList(3, context.args().size()));

        TwitchBroadcastMessage created = repository.add(channel, text, Math.max(intervalSeconds, 30),
                Math.max(minMessages, 0), context.message().userLogin());
        eventLogService.record(channel, context.message().userId(), "BROADCAST_ADDED",
                "id=" + created.id() + " intervalSeconds=" + created.intervalSeconds());
        context.reply("Broadcast #" + created.id() + " gespeichert (alle " + created.intervalSeconds()
                + "s, mind. " + created.minMessages() + " Chat-Nachrichten).");
    }

    private void handleRemove(TwitchCommandContext context, String channel) {
        Long id = parseId(context.arg(1));
        if (id == null) {
            context.reply("Nutzung: !broadcast remove <id>");
            return;
        }
        boolean removed = repository.remove(channel, id);
        if (removed) {
            eventLogService.record(channel, context.message().userId(), "BROADCAST_REMOVED", "id=" + id);
            context.reply("Broadcast #" + id + " wurde entfernt.");
        } else {
            context.reply("Es gibt keinen Broadcast mit der ID " + id + ".");
        }
    }

    private void handleToggle(TwitchCommandContext context, String channel, boolean enabled) {
        Long id = parseId(context.arg(1));
        if (id == null) {
            context.reply("Nutzung: !broadcast " + (enabled ? "enable" : "disable") + " <id>");
            return;
        }
        boolean changed = repository.setEnabled(channel, id, enabled);
        if (changed) {
            eventLogService.record(channel, context.message().userId(),
                    enabled ? "BROADCAST_ENABLED" : "BROADCAST_DISABLED", "id=" + id);
            context.reply("Broadcast #" + id + " ist jetzt " + (enabled ? "aktiv" : "pausiert") + ".");
        } else {
            context.reply("Es gibt keinen Broadcast mit der ID " + id + ".");
        }
    }

    private void handleList(TwitchCommandContext context, String channel) {
        List<TwitchBroadcastMessage> broadcasts = repository.list(channel);
        if (broadcasts.isEmpty()) {
            context.reply("Es sind noch keine Broadcasts fuer diesen Kanal angelegt.");
            return;
        }
        String joined = broadcasts.stream()
                .map(broadcast -> "#" + broadcast.id() + (broadcast.enabled() ? "" : " (pausiert)"))
                .collect(Collectors.joining(", "));
        context.reply("Broadcasts: " + joined);
    }

    private static Long parseId(String raw) {
        try {
            return raw == null ? null : Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static long parseLong(String raw, long fallback) {
        try {
            return raw == null ? fallback : Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
