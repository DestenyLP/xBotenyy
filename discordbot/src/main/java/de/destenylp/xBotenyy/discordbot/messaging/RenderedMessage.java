package de.destenylp.xBotenyy.discordbot.messaging;

import net.dv8tion.jda.api.entities.MessageEmbed;

public record RenderedMessage(String content, MessageEmbed embed) {
    public boolean isEmpty() {
        return (content == null || content.isBlank()) && embed == null;
    }
}
