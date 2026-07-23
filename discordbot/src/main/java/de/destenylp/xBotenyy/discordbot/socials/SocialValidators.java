package de.destenylp.xBotenyy.discordbot.socials;

import java.util.regex.Pattern;

public final class SocialValidators {
    private static final Pattern YOUTUBE_CHANNEL_ID = Pattern.compile("^UC[a-zA-Z0-9_-]{22}$");
    private static final Pattern TWITCH_LOGIN = Pattern.compile("^[a-zA-Z0-9_]{4,25}$");

    private SocialValidators() {
    }

    public static boolean isValidYoutubeChannelId(String value) {
        return value != null && YOUTUBE_CHANNEL_ID.matcher(value).matches();
    }

    public static boolean isValidTwitchLogin(String value) {
        return value != null && TWITCH_LOGIN.matcher(value).matches();
    }
}
