package de.destenylp.xBotenyy.twitchbot.commands.impl;

import de.destenylp.xBotenyy.common.commands.CommandPermission;
import de.destenylp.xBotenyy.twitchbot.commands.AbstractTwitchCommand;
import de.destenylp.xBotenyy.twitchbot.commands.TwitchCommandContext;
import de.destenylp.xBotenyy.twitchbot.eventlog.TwitchEventLogService;
import de.destenylp.xBotenyy.twitchbot.persistence.TwitchEventLogRepository.TwitchEventLogEntry;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class EventLogCommand extends AbstractTwitchCommand {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneOffset.UTC);
    private static final int DEFAULT_LIMIT = 5;

    private final TwitchEventLogService eventLogService;

    public EventLogCommand(TwitchEventLogService eventLogService) {
        super("eventlog", "Zeigt die letzten protokollierten Ereignisse dieses Kanals.", List.of("logs"),
                CommandPermission.BROADCASTER, 5);
        this.eventLogService = eventLogService;
    }

    @Override
    public void execute(TwitchCommandContext context) {
        List<TwitchEventLogEntry> entries = eventLogService.recent(context.message().channelLogin(), DEFAULT_LIMIT);
        if (entries.isEmpty()) {
            context.reply("Es wurden noch keine Ereignisse protokolliert.");
            return;
        }
        String joined = entries.stream()
                .map(entry -> TIME_FORMAT.format(Instant.ofEpochMilli(entry.createdAtEpochMillis())) + " "
                        + entry.eventType())
                .collect(Collectors.joining(" | "));
        context.reply("Letzte Ereignisse: " + joined);
    }
}
