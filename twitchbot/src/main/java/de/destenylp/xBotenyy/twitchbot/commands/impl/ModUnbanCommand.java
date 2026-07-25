package de.destenylp.xBotenyy.twitchbot.commands.impl;

import de.destenylp.xBotenyy.common.commands.CommandPermission;
import de.destenylp.xBotenyy.twitchbot.commands.AbstractTwitchCommand;
import de.destenylp.xBotenyy.twitchbot.commands.TwitchCommandContext;
import de.destenylp.xBotenyy.twitchbot.eventlog.TwitchEventLogService;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ModUnbanCommand extends AbstractTwitchCommand {
    private static final String USAGE = "Nutzung: !unban <nutzer>";

    private final TwitchEventLogService eventLogService;

    public ModUnbanCommand(TwitchEventLogService eventLogService) {
        super("unban", "Hebt einen Bann oder Timeout auf.", List.of("untimeout"),
                CommandPermission.MODERATOR, 2);
        this.eventLogService = eventLogService;
    }

    @Override
    public void execute(TwitchCommandContext context) {
        if (context.arg(0) == null) {
            context.reply(USAGE);
            return;
        }

        String targetLogin = context.arg(0).replace("@", "").toLowerCase(Locale.ROOT);
        String channel = context.message().channelLogin();
        Optional<String> broadcasterId = context.services().moderationApiClient().resolveUserId(channel);
        Optional<String> targetUserId = context.services().moderationApiClient().resolveUserId(targetLogin);
        if (broadcasterId.isEmpty() || targetUserId.isEmpty()) {
            context.reply("Konnte den Nutzer " + targetLogin + " nicht finden.");
            return;
        }

        boolean success = context.services().moderationApiClient().unbanUser(broadcasterId.get(),
                context.services().moderatorUserId(), targetUserId.get());
        if (success) {
            eventLogService.record(channel, context.message().userId(), "MANUAL_UNBAN",
                    "target=" + targetLogin + " by=" + context.message().userLogin());
            context.reply(targetLogin + " wurde entbannt.");
        } else {
            context.reply("Konnte " + targetLogin + " nicht entbannen.");
        }
    }
}
