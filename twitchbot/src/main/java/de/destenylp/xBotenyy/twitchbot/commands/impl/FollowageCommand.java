package de.destenylp.xBotenyy.twitchbot.commands.impl;

import de.destenylp.xBotenyy.twitchbot.commands.AbstractTwitchCommand;
import de.destenylp.xBotenyy.twitchbot.commands.TwitchCommandContext;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

public class FollowageCommand extends AbstractTwitchCommand {
    public FollowageCommand() {
        super("followage", "Zeigt, wie lange du diesem Kanal bereits folgst.");
    }

    @Override
    public void execute(TwitchCommandContext context) {
        String channelLogin = context.message().channelLogin();
        Optional<String> broadcasterId = context.services().moderationApiClient().resolveUserId(channelLogin);
        if (broadcasterId.isEmpty()) {
            context.reply("Konnte den Kanal " + channelLogin + " nicht aufloesen.");
            return;
        }

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

        Optional<Instant> followedAt = context.services().moderationApiClient()
                .getFollowedAt(broadcasterId.get(), targetUserId);
        if (followedAt.isEmpty()) {
            context.reply(targetDisplay + " folgt " + channelLogin + " (noch) nicht.");
            return;
        }

        Duration duration = Duration.between(followedAt.get(), Instant.now());
        context.reply(targetDisplay + " folgt " + channelLogin + " seit " + formatDuration(duration) + ".");
    }

    private static String formatDuration(Duration duration) {
        long totalDays = duration.toDays();
        long years = totalDays / 365;
        long months = (totalDays % 365) / 30;
        long days = (totalDays % 365) % 30;
        StringBuilder builder = new StringBuilder();
        if (years > 0) {
            builder.append(years).append(" Jahr(en) ");
        }
        if (years > 0 || months > 0) {
            builder.append(months).append(" Monat(en) ");
        }
        builder.append(days).append(" Tag(en)");
        return builder.toString();
    }
}
