package de.destenylp.xBotenyy.discordbot.messaging;

public class MessageTemplate {
    private boolean embed;
    private String title;
    private String titleUrl;
    private String author;
    private String content;
    private String color;
    private String imageUrl;
    private String footer;
    private boolean timestamp;

    private MessageTemplate(Builder builder) {
        this.embed = builder.embed;
        this.title = builder.title;
        this.titleUrl = builder.titleUrl;
        this.author = builder.author;
        this.content = builder.content;
        this.color = builder.color;
        this.imageUrl = builder.imageUrl;
        this.footer = builder.footer;
        this.timestamp = builder.timestamp;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isEmbed() {
        return embed;
    }

    public void setEmbed(boolean embed) {
        this.embed = embed;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitleUrl() {
        return titleUrl;
    }

    public void setTitleUrl(String titleUrl) {
        this.titleUrl = titleUrl;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public boolean isTimestamp() {
        return timestamp;
    }

    public void setTimestamp(boolean timestamp) {
        this.timestamp = timestamp;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getFooter() {
        return footer;
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }

    public static final class Builder {
        private boolean embed;
        private String title;
        private String titleUrl;
        private String author;
        private String content;
        private String color;
        private String imageUrl;
        private String footer;
        private boolean timestamp;

        private Builder() {
        }

        public Builder embed(boolean embed) {
            this.embed = embed;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder titleUrl(String titleUrl) {
            this.titleUrl = titleUrl;
            return this;
        }

        public Builder author(String author) {
            this.author = author;
            return this;
        }

        public Builder timestamp(boolean timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder color(String color) {
            this.color = color;
            return this;
        }

        public Builder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public Builder footer(String footer) {
            this.footer = footer;
            return this;
        }

        public MessageTemplate build() {
            return new MessageTemplate(this);
        }
    }
}
