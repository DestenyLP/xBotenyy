package de.destenylp.xBotenyy.discordbot.core;

import java.awt.Color;

public interface EntityStatus {
    String getLabel();

    String getEmoji();

    Color getColor();

    boolean isClosed();
}
