package de.destenylp.xBotenyy.twitchbot.commands.impl;

import de.destenylp.xBotenyy.twitchbot.commands.AbstractTwitchCommand;
import de.destenylp.xBotenyy.twitchbot.commands.TwitchCommandContext;

import java.time.Duration;
import java.time.Instant;

public class UptimeCommand extends AbstractTwitchCommand {
    public UptimeCommand() {
        super("uptime", "Zeigt, wie lange der Bot schon laeuft.");
    }

    @Override
    public void execute(TwitchCommandContext context) {
        Duration uptime = Duration.between(context.services().startedAt(), Instant.now());
        context.reply("Ich laufe seit " + format(uptime) + ".");
    }

    private static String format(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
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
