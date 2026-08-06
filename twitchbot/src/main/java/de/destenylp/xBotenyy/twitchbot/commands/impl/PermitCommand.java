package de.destenylp.xBotenyy.twitchbot.commands.impl;
import de.destenylp.xBotenyy.common.commands.CommandPermission;
import de.destenylp.xBotenyy.twitchbot.commands.AbstractTwitchCommand;
import de.destenylp.xBotenyy.twitchbot.commands.TwitchCommandContext;
import de.destenylp.xBotenyy.twitchbot.eventlog.TwitchEventLogService;
import java.util.List;
import java.util.Locale;
public class PermitCommand extends AbstractTwitchCommand {
    private static final String USAGE = "Nutzung: !permit <nutzer> [sekunden]";
    private final TwitchEventLogService eventLogService;
    private final long defaultDurationSeconds;
    public PermitCommand(TwitchEventLogService eventLogService, long defaultDurationSeconds) {
        super("permit", "Erlaubt einem Nutzer voruebergehend, den AutoMod-Filter zu umgehen.", List.of(),
                CommandPermission.MODERATOR, 1);
        this.eventLogService = eventLogService;
        this.defaultDurationSeconds = defaultDurationSeconds;
    }
    @Override
    public void execute(TwitchCommandContext context) {
        if (context.arg(0) == null) {
            context.reply(USAGE);
            return;
        }
        String targetLogin = context.arg(0).replace("@", "").toLowerCase(Locale.ROOT);
        long durationSeconds = defaultDurationSeconds;
        if (context.arg(1) != null) {
            try {
                durationSeconds = Long.parseLong(context.arg(1));
            } catch (NumberFormatException e) {
                context.reply(USAGE);
                return;
            }
        }
        if (durationSeconds <= 0) {
            context.reply(USAGE);
            return;
        }
        String channel = context.message().channelLogin();
        context.services().automodAdapter().permit(channel, targetLogin, durationSeconds);
        eventLogService.record(channel, context.message().userId(), "AUTOMOD_PERMIT",
                "target=" + targetLogin + " seconds=" + durationSeconds + " by=" + context.message().userLogin());
        context.reply(targetLogin + " darf den AutoMod-Filter fuer " + durationSeconds + " Sekunden umgehen.");
    }
}
