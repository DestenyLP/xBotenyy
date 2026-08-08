package de.destenylp.xBotenyy.discordbot.socials;

import de.destenylp.xBotenyy.discordbot.messaging.MessageTemplate;

public class SocialAccount {
    private static final String YOUTUBE_AUTHOR = "Neues Video!";
    private static final String YOUTUBE_TITLE = "{video.title}";
    private static final String YOUTUBE_TITLE_URL = "{video.url}";
    private static final String YOUTUBE_IMAGE = "{video.thumbnail}";
    private static final String YOUTUBE_FOOTER = "Veröffentlicht:";
    private static final String YOUTUBE_COLOR = "#FF0000";
    private static final String TWITCH_AUTHOR = "Livestream gestartet!";
    private static final String TWITCH_TITLE = "{stream.title}";
    private static final String TWITCH_TITLE_URL = "{stream.url}";
    private static final String TWITCH_IMAGE = "{stream.thumbnail}";
    private static final String TWITCH_FOOTER = "Live auf Twitch:";
    private static final String TWITCH_COLOR = "#9146FF";
    private static final String TIKTOK_AUTHOR = "Neues TikTok-Video!";
    private static final String TIKTOK_TITLE = "{tiktok.title}";
    private static final String TIKTOK_TITLE_URL = "{tiktok.url}";
    private static final String TIKTOK_IMAGE = "{tiktok.thumbnail}";
    private static final String TIKTOK_FOOTER = "Veroeffentlicht auf TikTok:";
    private static final String TIKTOK_COLOR = "#000000";
    private static final String DEFAULT_PING = "@everyone";
    private final MessageTemplate youtubeTemplate;
    private final MessageTemplate twitchTemplate;
    private final MessageTemplate tiktokTemplate;
    private String id;
    private String name;
    private String channelId;
    private boolean enabled;
    private String youtubeChannelId;
    private String lastYoutubeVideoId;
    private String twitchLogin;
    private String lastTwitchStreamId;
    private boolean twitchCurrentlyLive;
    private String tiktokUsername;
    private String lastTiktokVideoId;

    private SocialAccount(Builder builder) {
        this.name = builder.name;
        this.channelId = builder.channelId;
        this.enabled = true;
        this.youtubeChannelId = builder.youtubeChannelId;
        this.youtubeTemplate = MessageTemplate.builder()
                .embed(true)
                .author(YOUTUBE_AUTHOR)
                .title(YOUTUBE_TITLE)
                .titleUrl(YOUTUBE_TITLE_URL)
                .imageUrl(YOUTUBE_IMAGE)
                .footer(YOUTUBE_FOOTER)
                .timestamp(true)
                .color(YOUTUBE_COLOR)
                .content(builder.youtubeMessage)
                .ping(DEFAULT_PING)
                .build();
        this.twitchLogin = builder.twitchLogin;
        this.twitchTemplate = MessageTemplate.builder()
                .embed(true)
                .author(TWITCH_AUTHOR)
                .title(TWITCH_TITLE)
                .titleUrl(TWITCH_TITLE_URL)
                .imageUrl(TWITCH_IMAGE)
                .footer(TWITCH_FOOTER)
                .timestamp(true)
                .color(TWITCH_COLOR)
                .content(builder.twitchMessage)
                .ping(DEFAULT_PING)
                .build();
        this.tiktokUsername = builder.tiktokUsername;
        this.tiktokTemplate = MessageTemplate.builder()
                .embed(true)
                .author(TIKTOK_AUTHOR)
                .title(TIKTOK_TITLE)
                .titleUrl(TIKTOK_TITLE_URL)
                .imageUrl(TIKTOK_IMAGE)
                .footer(TIKTOK_FOOTER)
                .timestamp(true)
                .color(TIKTOK_COLOR)
                .content(builder.tiktokMessage)
                .ping(DEFAULT_PING)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    void assignId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean hasYoutube() {
        return youtubeChannelId != null;
    }

    public String getYoutubeChannelId() {
        return youtubeChannelId;
    }

    public void setYoutubeChannelId(String youtubeChannelId) {
        this.youtubeChannelId = youtubeChannelId;
        this.lastYoutubeVideoId = null;
    }

    public void clearYoutube() {
        this.youtubeChannelId = null;
        this.lastYoutubeVideoId = null;
    }

    public String getLastYoutubeVideoId() {
        return lastYoutubeVideoId;
    }

    public void setLastYoutubeVideoId(String lastYoutubeVideoId) {
        this.lastYoutubeVideoId = lastYoutubeVideoId;
    }

    public MessageTemplate getYoutubeTemplate() {
        return youtubeTemplate;
    }

    public boolean hasTwitch() {
        return twitchLogin != null;
    }

    public String getTwitchLogin() {
        return twitchLogin;
    }

    public void setTwitchLogin(String twitchLogin) {
        this.twitchLogin = twitchLogin;
        this.lastTwitchStreamId = null;
        this.twitchCurrentlyLive = false;
    }

    public void clearTwitch() {
        this.twitchLogin = null;
        this.lastTwitchStreamId = null;
        this.twitchCurrentlyLive = false;
    }

    public String getLastTwitchStreamId() {
        return lastTwitchStreamId;
    }

    public boolean isTwitchCurrentlyLive() {
        return twitchCurrentlyLive;
    }

    public void markTwitchLive(String streamId) {
        this.lastTwitchStreamId = streamId;
        this.twitchCurrentlyLive = true;
    }

    public void markTwitchOffline() {
        this.twitchCurrentlyLive = false;
    }

    public MessageTemplate getTwitchTemplate() {
        return twitchTemplate;
    }

    public boolean hasTiktok() {
        return tiktokUsername != null;
    }

    public String getTiktokUsername() {
        return tiktokUsername;
    }

    public void setTiktokUsername(String tiktokUsername) {
        this.tiktokUsername = tiktokUsername;
        this.lastTiktokVideoId = null;
    }

    public void clearTiktok() {
        this.tiktokUsername = null;
        this.lastTiktokVideoId = null;
    }

    public String getLastTiktokVideoId() {
        return lastTiktokVideoId;
    }

    public void setLastTiktokVideoId(String lastTiktokVideoId) {
        this.lastTiktokVideoId = lastTiktokVideoId;
    }

    public MessageTemplate getTiktokTemplate() {
        return tiktokTemplate;
    }

    public static final class Builder {
        private String name;
        private String channelId;
        private String youtubeChannelId;
        private String youtubeMessage;
        private String twitchLogin;
        private String twitchMessage;
        private String tiktokUsername;
        private String tiktokMessage;

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder channelId(String channelId) {
            this.channelId = channelId;
            return this;
        }

        public Builder youtubeChannelId(String youtubeChannelId) {
            this.youtubeChannelId = youtubeChannelId;
            return this;
        }

        public Builder youtubeMessage(String youtubeMessage) {
            this.youtubeMessage = youtubeMessage;
            return this;
        }

        public Builder twitchLogin(String twitchLogin) {
            this.twitchLogin = twitchLogin;
            return this;
        }

        public Builder twitchMessage(String twitchMessage) {
            this.twitchMessage = twitchMessage;
            return this;
        }

        public Builder tiktokUsername(String tiktokUsername) {
            this.tiktokUsername = tiktokUsername;
            return this;
        }

        public Builder tiktokMessage(String tiktokMessage) {
            this.tiktokMessage = tiktokMessage;
            return this;
        }

        public SocialAccount build() {
            return new SocialAccount(this);
        }
    }
}

