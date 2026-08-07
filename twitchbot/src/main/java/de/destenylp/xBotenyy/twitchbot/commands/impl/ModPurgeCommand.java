package de.destenylp.xBotenyy.twitchbot.commands.impl;

import de.destenylp.xBotenyy.common.commands.CommandPermission;
import de.destenylp.xBotenyy.twitchbot.commands.AbstractTwitchCommand;
import de.destenylp.xBotenyy.twitchbot.commands.TwitchCommandContext;
import de.destenylp.xBotenyy.twitchbot.eventlog.TwitchEventLogService;

import java.util.List;
import java.util.Optional;

public class ModPurgeCommand extends AbstractTwitchCommand {
    private final TwitchEventLogService eventLogService;

    public ModPurgeCommand(TwitchEventLogService eventLogService) {
        super("purge", "Leert den kompletten Chatverlauf des Kanals.", List.of("clear"),
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
        boolean success = context.services().moderationApiClient().clearChat(broadcasterId.get(),
                context.services().moderatorUserId());
        if (success) {
            eventLogService.record(channel, context.message().userId(), "MANUAL_CHAT_PURGE",
                    "by=" + context.message().userLogin());
            context.reply("Der Chat wurde geleert.");
        } else {
            context.reply("Konnte den Chat nicht leeren.");
        }
    }
}

