package de.destenylp.xBotenyy.common.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public record CommonConfig(
        String discordBotToken,
        String twitchClientId,
        String twitchClientSecret,
        String twitchChatBotUsername,
        String twitchChatOauthToken,
        String twitchModeratorAccessToken,
        String twitchBotRefreshToken,
        java.nio.file.Path envFilePath,
        String groqApiKey) {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommonConfig.class);
    private static final String ENV_FILE_NAME = ".env";
    private static final String ENV_DIR_OVERRIDE_VAR = "XBOTENYY_ENV_DIR";

    public static Optional<CommonConfig> load() {
        String envDirectory = resolveEnvDirectory();
        Dotenv dotenv = Dotenv.configure()
                .directory(envDirectory)
                .filename(ENV_FILE_NAME)
                .ignoreIfMissing()
                .load();

        String discordBotToken = blankToNull(resolve("BOT_TOKEN", dotenv));
        String twitchClientId = blankToNull(resolve("TWITCH_CLIENT_ID", dotenv));
        String twitchClientSecret = blankToNull(resolve("TWITCH_CLIENT_SECRET", dotenv));
        String twitchChatBotUsername = blankToNull(resolve("TWITCH_BOT_USERNAME", dotenv));
        String twitchChatOauthToken = blankToNull(resolve("TWITCH_BOT_OAUTH_TOKEN", dotenv));
        String twitchModeratorAccessToken = blankToNull(resolve("TWITCH_MODERATOR_ACCESS_TOKEN", dotenv));
        String twitchBotRefreshToken = blankToNull(resolve("TWITCH_BOT_REFRESH_TOKEN", dotenv));
        String groqApiKey = blankToNull(resolve("GROQ_API_KEY", dotenv));
        java.nio.file.Path envFilePath = java.nio.file.Path.of(envDirectory, ENV_FILE_NAME);

        return Optional.of(new CommonConfig(discordBotToken, twitchClientId, twitchClientSecret,
                twitchChatBotUsername, twitchChatOauthToken, twitchModeratorAccessToken,
                twitchBotRefreshToken, envFilePath, groqApiKey));
    }

    private static String resolveEnvDirectory() {
        String override = System.getenv(ENV_DIR_OVERRIDE_VAR);
        if (override == null || override.isBlank()) {
            override = System.getProperty("xbotenyy.env.dir");
        }
        if (override != null && !override.isBlank()) {
            return override;
        }

        for (String candidate : candidateDirectories()) {
            if (Files.isRegularFile(Path.of(candidate, ENV_FILE_NAME))) {
                return candidate;
            }
        }

        LOGGER.debug("Keine .env in den bekannten Kandidatenverzeichnissen gefunden, verwende '{}' als Fallback.",
                candidateDirectories().get(0));
        return candidateDirectories().get(0);
    }

    private static List<String> candidateDirectories() {
        return List.of("common", ".", "..", "../common", "../..", "../../common");
    }

    private static String resolve(String key, Dotenv dotenv) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            value = dotenv.get(key);
        }
        return value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public boolean hasDiscordToken() {
        return discordBotToken != null;
    }

    public boolean hasTwitchAppCredentials() {
        return twitchClientId != null && twitchClientSecret != null;
    }

    public boolean hasTwitchChatCredentials() {
        return twitchClientId != null && twitchClientSecret != null && twitchChatBotUsername != null;
    }

    public boolean hasTwitchModeratorAccessToken() {
        return twitchModeratorAccessToken != null;
    }

    public boolean hasTwitchBotRefreshToken() {
        return twitchBotRefreshToken != null && twitchClientSecret != null;
    }

    public boolean hasGroqApiKey() {
        return groqApiKey != null;
    }
}