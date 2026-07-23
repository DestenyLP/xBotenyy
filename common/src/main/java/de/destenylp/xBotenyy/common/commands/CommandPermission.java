package de.destenylp.xBotenyy.common.commands;

public enum CommandPermission {
    EVERYONE(0),
    SUBSCRIBER(1),
    VIP(2),
    MODERATOR(3),
    BROADCASTER(4),
    ADMIN(5);

    private final int level;

    CommandPermission(int level) {
        this.level = level;
    }

    public boolean isAtLeast(CommandPermission required) {
        return this.level >= required.level;
    }
}
