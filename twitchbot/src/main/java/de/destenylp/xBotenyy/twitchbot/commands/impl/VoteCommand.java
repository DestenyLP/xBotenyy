package de.destenylp.xBotenyy.twitchbot.commands.impl;

import de.destenylp.xBotenyy.common.commands.CommandPermission;
import de.destenylp.xBotenyy.twitchbot.commands.AbstractTwitchCommand;
import de.destenylp.xBotenyy.twitchbot.commands.TwitchCommandContext;
import de.destenylp.xBotenyy.twitchbot.poll.TwitchPoll;
import de.destenylp.xBotenyy.twitchbot.poll.TwitchPollManager;

import java.util.List;
import java.util.Optional;

public class VoteCommand extends AbstractTwitchCommand {
    private final TwitchPollManager pollManager;

    public VoteCommand(TwitchPollManager pollManager) {
        super("vote", "Stimmt bei der aktuell laufenden Umfrage ab.", List.of("v"), CommandPermission.EVERYONE, 1);
        this.pollManager = pollManager;
    }

    @Override
    public void execute(TwitchCommandContext context) {
        String channel = context.message().channelLogin();
        Optional<TwitchPoll> poll = pollManager.get(channel);
        if (poll.isEmpty()) {
            context.reply("Aktuell laeuft keine Umfrage in diesem Kanal.");
            return;
        }

        Integer optionNumber = parseNumber(context.arg(0));
        if (optionNumber == null || !poll.get().isValidOption(optionNumber)) {
            context.reply("Nutzung: !vote <nummer> " + poll.get().formatOptions());
            return;
        }

        poll.get().vote(context.message().userId(), optionNumber);
        context.reply(context.message().displayName() + " hat fuer Option " + optionNumber + " gestimmt.");
    }

    private static Integer parseNumber(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
