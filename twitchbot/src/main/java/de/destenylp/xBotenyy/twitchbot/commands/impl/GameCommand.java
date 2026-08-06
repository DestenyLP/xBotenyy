package de.destenylp.xBotenyy.twitchbot.commands.impl;
import de.destenylp.xBotenyy.common.commands.CommandPermission;
import de.destenylp.xBotenyy.twitchbot.commands.AbstractTwitchCommand;
import de.destenylp.xBotenyy.twitchbot.commands.TwitchCommandContext;
import de.destenylp.xBotenyy.twitchbot.eventlog.TwitchEventLogService;
import java.util.List;
import java.util.Optional;
public class GameCommand extends AbstractTwitchCommand {
    private final TwitchEventLogService eventLogService;
    public GameCommand(TwitchEventLogService eventLogService) {
        super("game", "Zeigt oder aendert die Stream-Kategorie.", List.of("category"),
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
                    .ifPresentOrElse(info -> context.reply("Aktuelle Kategorie: " + info.gameName()),
                            () -> context.reply("Konnte die aktuelle Kategorie nicht abrufen."));
            return;
        }
        if (!context.services().moderationApiClient().hasBroadcasterAccessToken()) {
            context.reply("Der Broadcaster-Zugriff ist nicht eingerichtet, die Kategorie kann nicht geaendert werden.");
            return;
        }
        String gameName = context.joinedArgs();
        Optional<String> gameId = context.services().moderationApiClient().resolveGameId(gameName);
        if (gameId.isEmpty()) {
            context.reply("Konnte die Kategorie " + gameName + " nicht finden.");
            return;
        }
        boolean success = context.services().moderationApiClient().updateChannelInformation(broadcasterId.get(),
                null, gameId.get());
        if (success) {
            eventLogService.record(channel, context.message().userId(), "GAME_CHANGED",
                    "game=" + gameName + " by=" + context.message().userLogin());
            context.reply("Kategorie geaendert zu: " + gameName);
        } else {
            context.reply("Konnte die Kategorie nicht aendern.");
        }
    }
}
