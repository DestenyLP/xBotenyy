package de.destenylp.xBotenyy.launcher.bot;

import de.destenylp.xBotenyy.common.config.CommonConfig;
import de.destenylp.xBotenyy.discordbot.Bot;
import de.destenylp.xBotenyy.discordbot.config.BotProperties;
import de.destenylp.xBotenyy.launcher.LauncherSettings;
import org.slf4j.Logger;

public final class DiscordManagedBot extends AbstractManagedBot<Bot> {
    public DiscordManagedBot(Logger logger, LauncherSettings settings) {
        super(BotId.DISCORD, "Discord-Bot", logger, settings);
    }

    @Override
    protected Bot createInstance() {
        CommonConfig config = CommonConfig.load()
                .orElseThrow(() -> new IllegalStateException("Die .env Konfiguration konnte nicht geladen werden."));
        if (!config.hasDiscordToken()) {
            throw new IllegalStateException("BOT_TOKEN ist nicht gesetzt - Discord-Bot kann nicht gestartet werden.");
        }
        BotProperties properties = BotProperties.load();
        return new Bot(config, properties);
    }

    @Override
    protected void performStart(Bot instance) throws Exception {
        instance.start();
    }
}

