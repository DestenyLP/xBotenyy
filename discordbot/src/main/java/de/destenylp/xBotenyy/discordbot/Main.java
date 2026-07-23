package de.destenylp.xBotenyy.discordbot;

import de.destenylp.xBotenyy.common.config.CommonConfig;
import de.destenylp.xBotenyy.discordbot.config.BotProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private Main() {
    }

    public static void main(String[] args) {
        BotProperties properties = BotProperties.load();
        CommonConfig config = CommonConfig.load().orElseThrow();
        if (!config.hasDiscordToken()) {
            LOGGER.error("Bot token is empty.");
            return;
        }
        try {
            new Bot(config, properties).start();
        } catch (Exception e) {
            LOGGER.error("Error while starting the Discord Bot: ", e);
        }
    }
}
