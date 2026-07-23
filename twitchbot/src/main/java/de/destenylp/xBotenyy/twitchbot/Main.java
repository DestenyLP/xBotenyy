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
            LOGGER.error("Twitch-Chat-Zugangsdaten fehlen. Bitte TWITCH_CLIENT_ID, TWITCH_CLIENT_SECRET "
                    + "und TWITCH_BOT_USERNAME in der .env setzen (siehe README).");
            return;
        }
        if (!config.hasTwitchModeratorAccessToken() && !config.hasTwitchBotRefreshToken()) {
            LOGGER.error("Weder TWITCH_MODERATOR_ACCESS_TOKEN noch TWITCH_BOT_REFRESH_TOKEN gesetzt - ohne "
                    + "einen davon kann AutoMod keine Nachrichten löschen oder Nutzer timeouten/bannen "
                    + "(siehe README, TWITCH_BOT_REFRESH_TOKEN wird empfohlen da er sich selbst erneuert).");
            return;
        }

        Bot bot = new Bot(config, properties);

        try {
            bot.start();
        } catch (Exception e) {
            LOGGER.error("Fehler beim Starten des Twitch-Bots: ", e);
            return;
        }

        try {
            bot.awaitShutdown();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}