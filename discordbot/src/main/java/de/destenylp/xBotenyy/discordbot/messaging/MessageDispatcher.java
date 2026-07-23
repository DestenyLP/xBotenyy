package de.destenylp.xBotenyy.discordbot.messaging;

import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;

import java.util.Optional;

public final class MessageDispatcher {
    private MessageDispatcher() {
    }

    public static Optional<MessageCreateAction> prepare(MessageChannel channel, RenderedMessage message) {
        if (message.isEmpty()) {
            return Optional.empty();
        }

        boolean hasContent = message.content() != null && !message.content().isBlank();

        MessageCreateAction action = hasContent
                ? channel.sendMessage(message.content())
                : channel.sendMessageEmbeds(message.embed());

        if (hasContent && message.embed() != null) {
            action = action.addEmbeds(message.embed());
        }

        return Optional.of(action);
    }

    public static Optional<MessageEditAction> prepareEdit(MessageChannel channel, String messageId, RenderedMessage message) {
        if (message.isEmpty()) {
            return Optional.empty();
        }

        boolean hasContent = message.content() != null && !message.content().isBlank();

        MessageEditAction action = hasContent
                ? channel.editMessageById(messageId, message.content())
                : channel.editMessageEmbedsById(messageId, message.embed());

        action = action.setReplace(true);

        if (hasContent && message.embed() != null) {
            action = action.setEmbeds(message.embed());
        }

        return Optional.of(action);
    }
}
