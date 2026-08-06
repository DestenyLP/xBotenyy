package de.destenylp.xBotenyy.twitchbot.commands.impl;
import de.destenylp.xBotenyy.common.commands.CommandPermission;
import de.destenylp.xBotenyy.twitchbot.commands.AbstractTwitchCommand;
import de.destenylp.xBotenyy.twitchbot.commands.TwitchCommandContext;
import java.util.List;
import java.util.Optional;
public class ClipCommand extends AbstractTwitchCommand {
    public ClipCommand() {
        super("clip", "Erstellt einen Clip vom aktuellen Stream-Moment.", List.of(),
                CommandPermission.EVERYONE, 30);
    }
    @Override
    public void execute(TwitchCommandContext context) {
        String channel = context.message().channelLogin();
        Optional<String> broadcasterId = context.services().moderationApiClient().resolveUserId(channel);
        if (broadcasterId.isEmpty()) {
            context.reply("Konnte den Kanal " + channel + " nicht aufloesen.");
            return;
        }
        Optional<String> clipId = context.services().moderationApiClient().createClip(broadcasterId.get());
        if (clipId.isPresent()) {
            context.reply("Clip wird erstellt: https://clips.twitch.tv/" + clipId.get());
        } else {
            context.reply("Konnte keinen Clip erstellen. Laeuft der Stream gerade?");
        }
    }
}
