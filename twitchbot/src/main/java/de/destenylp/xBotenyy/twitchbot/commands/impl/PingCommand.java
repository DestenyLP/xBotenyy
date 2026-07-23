package de.destenylp.xBotenyy.twitchbot.commands.impl;

import de.destenylp.xBotenyy.twitchbot.commands.AbstractTwitchCommand;
import de.destenylp.xBotenyy.twitchbot.commands.TwitchCommandContext;

public class PingCommand extends AbstractTwitchCommand {
    public PingCommand() {
        super("ping", "Prüft, ob der Bot erreichbar ist.");
    }

    @Override
    public void execute(TwitchCommandContext context) {
        context.reply("Pong! 🏓 Ich bin da, " + context.message().displayName() + ".");
    }
}
