package de.destenylp.xBotenyy.discordbot.welcome;

import de.destenylp.xBotenyy.discordbot.messaging.MessageTemplate;

public class WelcomeVariant {
    private String id;
    private boolean ping;
    private final MessageTemplate template;

    private WelcomeVariant(Builder builder) {
        this.id = builder.id;
        this.ping = builder.ping;
        this.template = MessageTemplate.builder()
                .embed(builder.embed)
                .title(builder.title)
                .content(builder.content)
                .color(builder.color)
                .imageUrl(builder.imageUrl)
                .footer(builder.footer)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public MessageTemplate getTemplate() {
        return template;
    }

    public String getId() {
        return id;
    }

    void assignId(String id) {
        this.id = id;
    }

    public boolean isEmbed() {
        return template.isEmbed();
    }

    public void setEmbed(boolean embed) {
        template.setEmbed(embed);
    }

    public String getTitle() {
        return template.getTitle();
    }

    public void setTitle(String title) {
        template.setTitle(title);
    }

    public String getContent() {
        return template.getContent();
    }

    public void setContent(String content) {
        template.setContent(content);
    }

    public String getColor() {
        return template.getColor();
    }

    public void setColor(String color) {
        template.setColor(color);
    }

    public String getImageUrl() {
        return template.getImageUrl();
    }

    public void setImageUrl(String imageUrl) {
        template.setImageUrl(imageUrl);
    }

    public String getFooter() {
        return template.getFooter();
    }

    public void setFooter(String footer) {
        template.setFooter(footer);
    }

    public boolean isPing() {
        return ping;
    }

    public void setPing(boolean ping) {
        this.ping = ping;
    }

    public static final class Builder {
        private String id;
        private boolean embed;
        private String title;
        private String content;
        private String color;
        private String imageUrl;
        private String footer;
        private boolean ping = true;

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder embed(boolean embed) {
            this.embed = embed;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
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

        public Builder ping(boolean ping) {
            this.ping = ping;
            return this;
        }

        public WelcomeVariant build() {
            return new WelcomeVariant(this);
        }
    }
}
