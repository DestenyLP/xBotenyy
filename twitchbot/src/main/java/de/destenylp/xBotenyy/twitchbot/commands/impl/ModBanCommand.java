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

public class ModBanCommand extends AbstractTwitchCommand {
    private static final String USAGE = "Nutzung: !ban <nutzer> [grund]";

    private final TwitchEventLogService eventLogService;
    private final ModerationCaseRepository caseRepository;
    private final TwitchModerationSyncTrigger syncTrigger;

    public ModBanCommand(TwitchEventLogService eventLogService, ModerationCaseRepository caseRepository,
                         TwitchModerationSyncTrigger syncTrigger) {
        super("ban", "Bannt einen Nutzer dauerhaft aus dem Kanal.", List.of(),
                CommandPermission.MODERATOR, 2);
        this.eventLogService = eventLogService;
        this.caseRepository = caseRepository;
        this.syncTrigger = syncTrigger;
    }

    @Override
    public void execute(TwitchCommandContext context) {
        if (context.arg(0) == null) {
            context.reply(USAGE);
            return;
        }

        String targetLogin = context.arg(0).replace("@", "").toLowerCase(Locale.ROOT);
        String reason = context.args().size() > 1
                ? String.join(" ", context.args().subList(1, context.args().size()))
                : "Bann durch " + context.message().displayName();

        String channel = context.message().channelLogin();
        Optional<String> broadcasterId = context.services().moderationApiClient().resolveUserId(channel);
        Optional<String> targetUserId = context.services().moderationApiClient().resolveUserId(targetLogin);
        if (broadcasterId.isEmpty() || targetUserId.isEmpty()) {
            context.reply("Konnte den Nutzer " + targetLogin + " nicht finden.");
            return;
        }

        boolean success = context.services().moderationApiClient().banUser(broadcasterId.get(),
                context.services().moderatorUserId(), targetUserId.get(), reason, 0);
        if (success) {
            eventLogService.record(channel, context.message().userId(), "MANUAL_BAN",
                    "target=" + targetLogin + " by=" + context.message().userLogin());
            caseRepository.insert(ModerationPlatform.TWITCH, channel, targetUserId.get(), targetLogin,
                    context.message().userId(), context.message().displayName(), ModerationAction.BAN, reason, 0, false);
            syncTrigger.trigger(targetUserId.get(), ModerationAction.BAN, reason, 0, context.message().displayName());
            context.reply(targetLogin + " wurde gebannt.");
        } else {
            context.reply("Konnte " + targetLogin + " nicht bannen.");
        }
    }
}
