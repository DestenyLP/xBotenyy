package de.destenylp.xBotenyy.twitchbot.commands.impl;

import de.destenylp.xBotenyy.twitchbot.commands.AbstractTwitchCommand;
import de.destenylp.xBotenyy.twitchbot.commands.TwitchCommandContext;

public class StrikesCommand extends AbstractTwitchCommand {
    public StrikesCommand() {
        super("strikes", "Zeigt deine aktuellen AutoMod-Strikes in diesem Kanal.");
    }

    @Override
    public void execute(TwitchCommandContext context) {
        int strikes = context.services().automodEngine()
                .getCurrentStrikes(context.message().channelLogin(), context.message().userId());
        if (strikes <= 0) {
            context.reply(context.message().displayName() + ", du hast aktuell keine Strikes.");
        } else {
            context.reply(context.message().displayName() + ", du hast aktuell " + strikes + " Strike(s).");
        }
    }
}
