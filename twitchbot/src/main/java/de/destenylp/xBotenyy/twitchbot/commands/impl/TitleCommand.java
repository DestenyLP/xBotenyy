package de.destenylp.xBotenyy.twitchbot.commands.impl;

import de.destenylp.xBotenyy.common.commands.CommandPermission;
import de.destenylp.xBotenyy.twitchbot.commands.AbstractTwitchCommand;
import de.destenylp.xBotenyy.twitchbot.commands.TwitchCommandContext;
import de.destenylp.xBotenyy.twitchbot.eventlog.TwitchEventLogService;

import java.util.List;
import java.util.Optional;

public class TitleCommand extends AbstractTwitchCommand {
    private final TwitchEventLogService eventLogService;

    public TitleCommand(TwitchEventLogService eventLogService) {
        super("title", "Zeigt oder aendert den Stream-Titel.", List.of(),
                CommandPermission.MODERATOR, 5);
        this.eventLogService = eventLogService;
    }

    @Override
    public void execute(TwitchCommandContext context) {
        String channel = context.message().channelLogin();
        Optional<String> broadcasterId = context.services().moderationApiClient().resolveUserId(channel);
        if (broadcasterId.isEmpty()) {
            context.reply("Konnte den Kanal " + channel + " nicht aufloesen.");
            return;
        }
        if (context.args().isEmpty()) {
            context.services().moderationApiClient().getChannelInformation(broadcasterId.get())
                    .ifPresentOrElse(info -> context.reply("Aktueller Titel: " + info.title()),
                            () -> context.reply("Konnte den aktuellen Titel nicht abrufen."));
            return;
        }
        if (!context.services().moderationApiClient().hasBroadcasterAccessToken()) {
            context.reply("Der Broadcaster-Zugriff ist nicht eingerichtet, der Titel kann nicht geaendert werden.");
            return;
        }
        String newTitle = context.joinedArgs();
        boolean success = context.services().moderationApiClient().updateChannelInformation(broadcasterId.get(),
                newTitle, null);
        if (success) {
            eventLogService.record(channel, context.message().userId(), "TITLE_CHANGED",
                    "title=" + newTitle + " by=" + context.message().userLogin());
            context.reply("Titel geaendert zu: " + newTitle);
        } else {
            context.reply("Konnte den Titel nicht aendern.");
        }
    }
}

