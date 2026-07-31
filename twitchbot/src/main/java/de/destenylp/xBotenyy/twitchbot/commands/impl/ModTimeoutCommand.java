package de.destenylp.xBotenyy.twitchbot.commands.impl;

import de.destenylp.xBotenyy.common.commands.CommandPermission;
import de.destenylp.xBotenyy.common.moderation.ModerationAction;
import de.destenylp.xBotenyy.common.moderation.ModerationCaseRepository;
import de.destenylp.xBotenyy.common.moderation.ModerationPlatform;
import de.destenylp.xBotenyy.twitchbot.commands.AbstractTwitchCommand;
import de.destenylp.xBotenyy.twitchbot.commands.TwitchCommandContext;
import de.destenylp.xBotenyy.twitchbot.eventlog.TwitchEventLogService;
import de.destenylp.xBotenyy.twitchbot.moderation.TwitchModerationSyncTrigger;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ModTimeoutCommand extends AbstractTwitchCommand {
    private static final String USAGE = "Nutzung: !timeout <nutzer> <sekunden> [grund]";
    private static final long MAX_DURATION_SECONDS = 1209600;

    private final TwitchEventLogService eventLogService;
    private final ModerationCaseRepository caseRepository;
    private final TwitchModerationSyncTrigger syncTrigger;

    public ModTimeoutCommand(TwitchEventLogService eventLogService, ModerationCaseRepository caseRepository,
                              TwitchModerationSyncTrigger syncTrigger) {
        super("timeout", "Timeoutet einen Nutzer fuer eine bestimmte Dauer.", List.of("to"),
                CommandPermission.MODERATOR, 2);
        this.eventLogService = eventLogService;
        this.caseRepository = caseRepository;
        this.syncTrigger = syncTrigger;
    }

    @Override
    public void execute(TwitchCommandContext context) {
        if (context.args().size() < 2) {
            context.reply(USAGE);
            return;
        }

        String targetLogin = context.arg(0).replace("@", "").toLowerCase(Locale.ROOT);
        long durationSeconds;
        try {
            durationSeconds = Math.min(Long.parseLong(context.arg(1)), MAX_DURATION_SECONDS);
        } catch (NumberFormatException e) {
            context.reply(USAGE);
            return;
        }
        if (durationSeconds <= 0) {
            context.reply(USAGE);
            return;
        }

        String reason = context.args().size() > 2
                ? String.join(" ", context.args().subList(2, context.args().size()))
                : "Timeout durch " + context.message().displayName();

        String channel = context.message().channelLogin();
        Optional<String> broadcasterId = context.services().moderationApiClient().resolveUserId(channel);
        Optional<String> targetUserId = context.services().moderationApiClient().resolveUserId(targetLogin);
        if (broadcasterId.isEmpty() || targetUserId.isEmpty()) {
            context.reply("Konnte den Nutzer " + targetLogin + " nicht finden.");
            return;
        }

        boolean success = context.services().moderationApiClient().banUser(broadcasterId.get(),
                context.services().moderatorUserId(), targetUserId.get(), reason, durationSeconds);
        if (success) {
            eventLogService.record(channel, context.message().userId(), "MANUAL_TIMEOUT",
                    "target=" + targetLogin + " seconds=" + durationSeconds + " by=" + context.message().userLogin());
            caseRepository.insert(ModerationPlatform.TWITCH, channel, targetUserId.get(), targetLogin,
                    context.message().userId(), context.message().displayName(), ModerationAction.TIMEOUT, reason,
                    durationSeconds, false);
            syncTrigger.trigger(targetUserId.get(), ModerationAction.TIMEOUT, reason, durationSeconds,
                    context.message().displayName());
            context.reply(targetLogin + " wurde fuer " + durationSeconds + " Sekunden getimeoutet.");
        } else {
            context.reply("Konnte " + targetLogin + " nicht timeouten.");
        }
    }
}
