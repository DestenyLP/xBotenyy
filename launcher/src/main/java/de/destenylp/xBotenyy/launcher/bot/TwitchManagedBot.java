package de.destenylp.xBotenyy.launcher.bot;

import de.destenylp.xBotenyy.common.config.CommonConfig;
import de.destenylp.xBotenyy.launcher.LauncherSettings;
import de.destenylp.xBotenyy.twitchbot.Bot;
import de.destenylp.xBotenyy.twitchbot.config.TwitchBotProperties;
import org.slf4j.Logger;

public final class TwitchManagedBot extends AbstractManagedBot<Bot> {

    public TwitchManagedBot(Logger logger, LauncherSettings settings) {
        super(BotId.TWITCH, "Twitch-Bot", logger, settings);
    }

    @Override
    protected Bot createInstance() {
        CommonConfig config = CommonConfig.load()
                .orElseThrow(() -> new IllegalStateException("Die .env Konfiguration konnte nicht geladen werden."));
        if (!config.hasTwitchChatCredentials()) {
            throw new IllegalStateException("Twitch-Chat-Zugangsdaten fehlen (TWITCH_CLIENT_ID, "
                    + "TWITCH_CLIENT_SECRET, TWITCH_BOT_USERNAME in der .env pruefen).");
        }
        if (!config.hasTwitchModeratorAccessToken() && !config.hasTwitchBotRefreshToken()) {
            throw new IllegalStateException("Weder TWITCH_MODERATOR_ACCESS_TOKEN noch TWITCH_BOT_REFRESH_TOKEN "
                    + "gesetzt (siehe README).");
        }
        TwitchBotProperties properties = TwitchBotProperties.load();
        return new Bot(config, properties);
    }

    @Override
    protected void performStart(Bot instance) throws Exception {
        instance.start();
    }
}
