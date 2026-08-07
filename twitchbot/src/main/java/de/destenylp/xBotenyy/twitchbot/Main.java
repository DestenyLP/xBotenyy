package de.destenylp.xBotenyy.twitchbot;

import de.destenylp.xBotenyy.common.config.CommonConfig;
import de.destenylp.xBotenyy.twitchbot.config.TwitchBotProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private Main() {
    }

    public static void main(String[] args) {
        TwitchBotProperties properties = TwitchBotProperties.load();
        CommonConfig config = CommonConfig.load().orElseThrow();
        if (!config.hasTwitchChatCredentials()) {
            LOGGER.error("Twitch chat credentials are missing. Please set TWITCH_CLIENT_ID, TWITCH_CLIENT_SECRET, "
                    + "and TWITCH_BOT_USERNAME in the .env file (see README).");
            return;
        }
        if (!config.hasTwitchModeratorAccessToken() && !config.hasTwitchBotRefreshToken()) {
            LOGGER.error("Neither TWITCH_MODERATOR_ACCESS_TOKEN nor TWITCH_BOT_REFRESH_TOKEN is set - without "
                    + "one of these, AutoMod cannot delete messages or time out/ban users "
                    + "(see README; TWITCH_BOT_REFRESH_TOKEN is recommended since it refreshes itself).");
            return;
        }
        Bot bot = new Bot(config, properties);
        try {
            bot.start();
        } catch (Exception e) {
            LOGGER.error("Error while starting the Twitch bot: ", e);
            return;
        }
        try {
            bot.awaitShutdown();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

