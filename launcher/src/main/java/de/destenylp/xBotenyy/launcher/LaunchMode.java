package de.destenylp.xBotenyy.launcher;

import java.util.Locale;

public enum LaunchMode {
    BOTH(true, true),
    DISCORD_ONLY(true, false),
    TWITCH_ONLY(false, true);

    private final boolean discord;
    private final boolean twitch;

    LaunchMode(boolean discord, boolean twitch) {
        this.discord = discord;
        this.twitch = twitch;
    }

    public static LaunchMode fromArgs(String[] args) {
        if (args.length == 0) {
            return BOTH;
        }
        String value = args[0].toLowerCase(Locale.ROOT).replace("--mode=", "").trim();
        return switch (value) {
            case "discord", "discordbot", "discord-only" -> DISCORD_ONLY;
            case "twitch", "twitchbot", "twitch-only" -> TWITCH_ONLY;
            default -> BOTH;
        };
    }

    public boolean includesDiscord() {
        return discord;
    }

    public boolean includesTwitch() {
        return twitch;
    }
}
