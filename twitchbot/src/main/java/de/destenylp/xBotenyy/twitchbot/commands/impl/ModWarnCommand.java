package de.destenylp.xBotenyy.twitchbot.commands.impl;
import de.destenylp.xBotenyy.common.commands.CommandPermission;
import de.destenylp.xBotenyy.common.moderation.ModerationAction;
import de.destenylp.xBotenyy.common.moderation.ModerationCaseRepository;
import de.destenylp.xBotenyy.common.moderation.ModerationPlatform;
import de.destenylp.xBotenyy.twitchbot.commands.AbstractTwitchCommand;
import de.destenylp.xBotenyy.twitchbot.commands.TwitchCommandContext;
import de.destenylp.xBotenyy.twitchbot.moderation.TwitchModerationSyncTrigger;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
public class ModWarnCommand extends AbstractTwitchCommand {
    private static final String USAGE = "Nutzung: !warn <nutzer> [grund]";
    private final ModerationCaseRepository caseRepository;
    private final TwitchModerationSyncTrigger syncTrigger;
    public ModWarnCommand(ModerationCaseRepository caseRepository, TwitchModerationSyncTrigger syncTrigger) {
        super("warn", "Verwarnt einen Nutzer manuell.", List.of(), CommandPermission.MODERATOR, 2);
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
                : "Verwarnung durch " + context.message().displayName();
        String channel = context.message().channelLogin();
        Optional<String> targetUserId = context.services().moderationApiClient().resolveUserId(targetLogin);
        if (targetUserId.isEmpty()) {
            context.reply("Konnte den Nutzer " + targetLogin + " nicht finden.");
            return;
        }
        caseRepository.insert(ModerationPlatform.TWITCH, channel, targetUserId.get(), targetLogin,
                context.message().userId(), context.message().displayName(), ModerationAction.WARN, reason, 0, false);
        int total = caseRepository.countActiveWarnings(ModerationPlatform.TWITCH, channel, targetUserId.get());
        syncTrigger.trigger(targetUserId.get(), ModerationAction.WARN, reason, 0, context.message().displayName());
        context.reply(targetLogin + " wurde verwarnt (" + total + ". Verwarnung).");
    }
}
