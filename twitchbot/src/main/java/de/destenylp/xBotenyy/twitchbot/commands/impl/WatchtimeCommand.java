package de.destenylp.xBotenyy.twitchbot.commands.impl;

import de.destenylp.xBotenyy.twitchbot.commands.AbstractTwitchCommand;
import de.destenylp.xBotenyy.twitchbot.commands.TwitchCommandContext;

import java.util.Locale;
import java.util.Optional;

public class WatchtimeCommand extends AbstractTwitchCommand {
    public WatchtimeCommand() {
        super("watchtime", "Zeigt deine bisherige Watchtime in diesem Kanal.");
    }

    @Override
    public void execute(TwitchCommandContext context) {
        String rawTarget = context.arg(0);
        String targetUserId;
        String targetDisplay;

        if (rawTarget == null) {
            targetUserId = context.message().userId();
            targetDisplay = context.message().displayName();
        } else {
            String normalized = rawTarget.replace("@", "").toLowerCase(Locale.ROOT);
            Optional<String> resolved = context.services().moderationApiClient().resolveUserId(normalized);
            if (resolved.isEmpty()) {
                context.reply("Konnte den Nutzer " + rawTarget + " nicht finden.");
                return;
            }
            targetUserId = resolved.get();
            targetDisplay = normalized;
        }

        long seconds = context.services().watchtimeRepository()
                .getSeconds(context.message().channelLogin(), targetUserId);
        context.reply(targetDisplay + " hat aktuell " + formatDuration(seconds) + " Watchtime in diesem Kanal.");
    }

    private static String formatDuration(long totalSeconds) {
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        StringBuilder builder = new StringBuilder();
        if (days > 0) {
            builder.append(days).append("d ");
        }
        if (days > 0 || hours > 0) {
            builder.append(hours).append("h ");
        }
        builder.append(minutes).append("m");
        return builder.toString();
    }
}
