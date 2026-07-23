package de.destenylp.xBotenyy.discordbot.reactionroles;

public class ReactionRoleEntry {
    private final String componentId;
    private final String roleId;
    private final ReactionRoleType type;
    private final String emoji;
    private final String buttonLabel;
    private final String buttonStyle;

    private ReactionRoleEntry(Builder builder) {
        this.componentId = builder.componentId;
        this.roleId = builder.roleId;
        this.type = builder.type;
        this.emoji = builder.emoji;
        this.buttonLabel = builder.buttonLabel;
        this.buttonStyle = builder.buttonStyle;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getComponentId() {
        return componentId;
    }

    public String getRoleId() {
        return roleId;
    }

    public ReactionRoleType getType() {
        return type;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getButtonLabel() {
        return buttonLabel;
    }

    public String getButtonStyle() {
        return buttonStyle;
    }

    public static final class Builder {
        private String componentId;
        private String roleId;
        private ReactionRoleType type;
        private String emoji;
        private String buttonLabel;
        private String buttonStyle;

        private Builder() {
        }

        public Builder componentId(String componentId) {
            this.componentId = componentId;
            return this;
        }

        public Builder roleId(String roleId) {
            this.roleId = roleId;
            return this;
        }

        public Builder type(ReactionRoleType type) {
            this.type = type;
            return this;
        }

        public Builder emoji(String emoji) {
            this.emoji = emoji;
            return this;
        }

        public Builder buttonLabel(String buttonLabel) {
            this.buttonLabel = buttonLabel;
            return this;
        }

        public Builder buttonStyle(String buttonStyle) {
            this.buttonStyle = buttonStyle;
            return this;
        }

        public ReactionRoleEntry build() {
            return new ReactionRoleEntry(this);
        }
    }
}
