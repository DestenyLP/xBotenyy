package de.destenylp.xBotenyy.discordbot.core;

import java.time.Duration;

public interface PrunableGuildService extends GuildService {
    int pruneOldEntries(Duration retention);
}
