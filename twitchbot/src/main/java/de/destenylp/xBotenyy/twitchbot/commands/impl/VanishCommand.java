package de.destenylp.xBotenyy.twitchbot.commands.impl;
import de.destenylp.xBotenyy.common.commands.CommandPermission;
import de.destenylp.xBotenyy.twitchbot.commands.AbstractTwitchCommand;
import de.destenylp.xBotenyy.twitchbot.commands.TwitchCommandContext;
import java.util.List;
import java.util.Optional;
public class VanishCommand extends AbstractTwitchCommand {
    private static final long VANISH_DURATION_SECONDS = 1;
    private static final String VANISH_REASON = "Vanish";
    public VanishCommand() {
        super("vanish", "Vanishe dich selbst!", List.of("v"),
                CommandPermission.EVERYONE, 30);
    }
    @Override
    public void execute(TwitchCommandContext context) throws Exception {
        String channelLogin = context.message().channelLogin();
        String userId = context.message().userId();
        Optional<String> broadcasterId = context.services().moderationApiClient().resolveUserId(channelLogin);
        if (broadcasterId.isEmpty()) {
            context.reply("Konnte den Kanal nicht aufloesen, vanish fehlgeschlagen.");
            return;
        }
        boolean success = context.services().moderationApiClient().banUser(broadcasterId.get(),
                context.services().moderatorUserId(), userId, VANISH_REASON, VANISH_DURATION_SECONDS);
        if (!success) {
            context.reply("@" + context.message().displayName()
                    + " kann sich nicht vanishen (Broadcaster/Moderatoren koennen von Twitch nicht getimeoutet werden).");
        }
    }
}