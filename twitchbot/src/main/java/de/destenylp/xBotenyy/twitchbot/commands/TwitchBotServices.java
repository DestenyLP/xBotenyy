package de.destenylp.xBotenyy.twitchbot.commands;

import de.destenylp.xBotenyy.common.automod.AutomodEngine;
import de.destenylp.xBotenyy.twitchbot.automod.TwitchModerationApiClient;
import de.destenylp.xBotenyy.twitchbot.chat.TwitchChatClient;
import de.destenylp.xBotenyy.twitchbot.persistence.CustomCommandRepository;
import de.destenylp.xBotenyy.twitchbot.persistence.TwitchWatchtimeRepository;

import java.time.Instant;

public record TwitchBotServices(
        TwitchChatClient chatClient,
        AutomodEngine automodEngine,
        CustomCommandRepository customCommandRepository,
        TwitchWatchtimeRepository watchtimeRepository,
        TwitchModerationApiClient moderationApiClient,
        String moderatorUserId,
        Instant startedAt) {
    public void reply(String channelLogin, String message) {
        chatClient.sendMessage(channelLogin, message);
    }
}
