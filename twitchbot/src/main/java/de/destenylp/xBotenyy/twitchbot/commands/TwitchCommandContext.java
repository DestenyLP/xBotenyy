package de.destenylp.xBotenyy.twitchbot.commands;

import de.destenylp.xBotenyy.twitchbot.chat.TwitchChatMessage;

import java.util.List;

public record TwitchCommandContext(TwitchChatMessage message, List<String> args, TwitchBotServices services) {
    public String joinedArgs() {
        return String.join(" ", args);
    }

    public String arg(int index) {
        return index < args.size() ? args.get(index) : null;
    }

    public void reply(String text) {
        services.reply(message.channelLogin(), text);
    }
}
