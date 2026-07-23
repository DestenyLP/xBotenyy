package de.destenylp.xBotenyy.discordbot.welcome;

import de.destenylp.xBotenyy.discordbot.util.FieldEdit;

public final class WelcomeVariantEdit {
    private final String content;
    private final Boolean embed;
    private final FieldEdit<String> title;
    private final FieldEdit<String> color;
    private final FieldEdit<String> imageUrl;
    private final FieldEdit<String> footer;
    private final Boolean ping;

    private WelcomeVariantEdit(Builder builder) {
        this.content = builder.content;
        this.embed = builder.embed;
        this.title = builder.title;
        this.color = builder.color;
        this.imageUrl = builder.imageUrl;
        this.footer = builder.footer;
        this.ping = builder.ping;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void applyTo(WelcomeVariant variant) {
        if (content != null) {
            variant.setContent(content);
        }
        if (embed != null) {
            variant.setEmbed(embed);
        }
        title.applyTo(variant::setTitle);
        color.applyTo(variant::setColor);
        imageUrl.applyTo(variant::setImageUrl);
        footer.applyTo(variant::setFooter);
        if (ping != null) {
            variant.setPing(ping);
        }
    }

    public static final class Builder {
        private String content;
        private Boolean embed;
        private FieldEdit<String> title = FieldEdit.notProvided();
        private FieldEdit<String> color = FieldEdit.notProvided();
        private FieldEdit<String> imageUrl = FieldEdit.notProvided();
        private FieldEdit<String> footer = FieldEdit.notProvided();
        private Boolean ping;

        private Builder() {
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder embed(Boolean embed) {
            this.embed = embed;
            return this;
        }

        public Builder title(FieldEdit<String> title) {
            this.title = title;
            return this;
        }

        public Builder color(FieldEdit<String> color) {
            this.color = color;
            return this;
        }

        public Builder imageUrl(FieldEdit<String> imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public Builder footer(FieldEdit<String> footer) {
            this.footer = footer;
            return this;
        }

        public Builder ping(Boolean ping) {
            this.ping = ping;
            return this;
        }

        public WelcomeVariantEdit build() {
            return new WelcomeVariantEdit(this);
        }
    }
}
