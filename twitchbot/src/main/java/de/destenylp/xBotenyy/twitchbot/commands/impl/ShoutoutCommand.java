package de.destenylp.xBotenyy.twitchbot.commands.impl;

import de.destenylp.xBotenyy.common.commands.CommandPermission;
import de.destenylp.xBotenyy.twitchbot.automod.TwitchModerationApiClient;
import de.destenylp.xBotenyy.twitchbot.commands.AbstractTwitchCommand;
import de.destenylp.xBotenyy.twitchbot.commands.TwitchCommandContext;
import de.destenylp.xBotenyy.twitchbot.eventlog.TwitchEventLogService;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ShoutoutCommand extends AbstractTwitchCommand {
    private static final String USAGE = "Nutzung: !so <kanal>";

    private final TwitchEventLogService eventLogService;

    public ShoutoutCommand(TwitchEventLogService eventLogService) {
        super("so", "Gibt einem anderen Streamer einen Shoutout.", List.of("shoutout"),
                CommandPermission.MODERATOR, 5);
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
        TwitchModerationApiClient moderationApiClient = context.services().moderationApiClient();

        Optional<String> broadcasterId = moderationApiClient.resolveUserId(channel);
        Optional<String> targetBroadcasterId = moderationApiClient.resolveUserId(targetLogin);
        if (broadcasterId.isEmpty() || targetBroadcasterId.isEmpty()) {
            context.reply("Konnte den Kanal " + targetLogin + " nicht finden.");
            return;
        }

        moderationApiClient.sendShoutout(broadcasterId.get(), targetBroadcasterId.get(),
                context.services().moderatorUserId());

        Optional<TwitchModerationApiClient.ChannelInfo> targetInfo =
                moderationApiClient.getChannelInformation(targetBroadcasterId.get());
        String message = targetInfo
                .map(info -> "Schaut unbedingt bei " + targetLogin + " vorbei! Zuletzt gestreamt: "
                        + (info.gameName().isBlank() ? "unbekannt" : info.gameName())
                        + " - https://twitch.tv/" + targetLogin)
                .orElse("Schaut unbedingt bei " + targetLogin + " vorbei! - https://twitch.tv/" + targetLogin);

        context.reply(message);
        eventLogService.record(channel, context.message().userId(), "SHOUTOUT",
                "target=" + targetLogin + " by=" + context.message().userLogin());
    }
}
