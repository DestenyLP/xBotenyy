package de.destenylp.xBotenyy.twitchbot.commands.impl;
import de.destenylp.xBotenyy.common.commands.CommandPermission;
import de.destenylp.xBotenyy.twitchbot.commands.AbstractTwitchCommand;
import de.destenylp.xBotenyy.twitchbot.commands.TwitchCommandContext;
import de.destenylp.xBotenyy.twitchbot.eventlog.TwitchEventLogService;
import de.destenylp.xBotenyy.twitchbot.poll.TwitchPoll;
import de.destenylp.xBotenyy.twitchbot.poll.TwitchPollManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
public class PollCommand extends AbstractTwitchCommand {
    private static final String USAGE = "Nutzung: !poll start <frage> | <option 1> | <option 2> [| ...] | "
            + "!poll results | !poll end";
    private static final int MIN_OPTIONS = 2;
    private static final int MAX_OPTIONS = 5;
    private final TwitchPollManager pollManager;
    private final TwitchEventLogService eventLogService;
    public PollCommand(TwitchPollManager pollManager, TwitchEventLogService eventLogService) {
        super("poll", "Startet, beendet oder wertet eine Chat-Umfrage aus.", List.of("umfrage"),
                CommandPermission.EVERYONE, 2);
        this.pollManager = pollManager;
        this.eventLogService = eventLogService;
    }
    @Override
    public void execute(TwitchCommandContext context) {
        String channel = context.message().channelLogin();
        String sub = context.arg(0) == null ? "" : context.arg(0).toLowerCase(Locale.ROOT);
        switch (sub) {
            case "start" -> handleStart(context, channel);
            case "end", "stop" -> handleEnd(context, channel);
            case "results", "result" -> handleResults(context, channel);
            default -> context.reply(USAGE);
        }
    }
    private void handleStart(TwitchCommandContext context, String channel) {
        if (!context.message().isPrivileged()) {
            context.reply("Nur Moderatoren koennen eine Umfrage starten.");
            return;
        }
        if (pollManager.isActive(channel)) {
            context.reply("Es laeuft bereits eine Umfrage in diesem Kanal. Beende sie zuerst mit !poll end.");
            return;
        }
        String rawArgs = context.joinedArgs();
        String withoutStart = rawArgs.length() > 5 ? rawArgs.substring(5).trim() : "";
        List<String> parts = new ArrayList<>();
        for (String part : withoutStart.split("\\|")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                parts.add(trimmed);
            }
        }
        if (parts.size() < MIN_OPTIONS + 1) {
            context.reply(USAGE);
            return;
        }
        String question = parts.get(0);
        List<String> options = parts.subList(1, parts.size());
        if (options.size() > MAX_OPTIONS) {
            context.reply("Maximal " + MAX_OPTIONS + " Optionen sind erlaubt.");
            return;
        }
        TwitchPoll poll = pollManager.start(channel, question, options, context.message().userLogin());
        eventLogService.record(channel, context.message().userId(), "POLL_STARTED", "question=" + question);
        context.reply("Umfrage gestartet: " + question + " -> " + poll.formatOptions()
                + " | Abstimmen mit !vote <nummer>");
    }
    private void handleEnd(TwitchCommandContext context, String channel) {
        if (!context.message().isPrivileged()) {
            context.reply("Nur Moderatoren koennen eine Umfrage beenden.");
            return;
        }
        Optional<TwitchPoll> poll = pollManager.end(channel);
        if (poll.isEmpty()) {
            context.reply("Es laeuft aktuell keine Umfrage in diesem Kanal.");
            return;
        }
        eventLogService.record(channel, context.message().userId(), "POLL_ENDED",
                "question=" + poll.get().question() + " votes=" + poll.get().totalVotes());
        context.reply("Umfrage beendet: " + poll.get().question() + " -> " + poll.get().formatResults()
                + " (" + poll.get().totalVotes() + " Stimme(n))");
    }
    private void handleResults(TwitchCommandContext context, String channel) {
        Optional<TwitchPoll> poll = pollManager.get(channel);
        if (poll.isEmpty()) {
            context.reply("Es laeuft aktuell keine Umfrage in diesem Kanal.");
            return;
        }
        context.reply(poll.get().question() + " -> " + poll.get().formatResults()
                + " (" + poll.get().totalVotes() + " Stimme(n))");
    }
}
