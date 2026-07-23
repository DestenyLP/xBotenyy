package de.destenylp.xBotenyy.discordbot.listeners;

import de.destenylp.xBotenyy.discordbot.reactionroles.ReactionRoleEntry;
import de.destenylp.xBotenyy.discordbot.reactionroles.ReactionRoleMessage;
import de.destenylp.xBotenyy.discordbot.reactionroles.ReactionRoleService;
import de.destenylp.xBotenyy.discordbot.reactionroles.ReactionRoleType;
import de.destenylp.xBotenyy.discordbot.util.RetryingRestAction;
import de.destenylp.xBotenyy.discordbot.observability.BotMetrics;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class ReactionRoleListener extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReactionRoleListener.class);

    private final ReactionRoleService service;

    public ReactionRoleListener(ReactionRoleService service) {
        this.service = service;
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        try {
            handleReactionAdd(event);
        } catch (Exception e) {
            LOGGER.error("Unerwarteter Fehler bei Reaction-Add auf Nachricht {}: ", event.getMessageId(), e);
        }
    }

    private void handleReactionAdd(MessageReactionAddEvent event) {
        if (!event.isFromGuild() || event.getUser() == null || event.getUser().isBot()) {
            return;
        }

        Guild guild = event.getGuild();
        Optional<ReactionRoleMessage> rrMessageOpt = service.findMessage(guild.getId(), event.getMessageId());
        if (rrMessageOpt.isEmpty()) {
            return;
        }

        String emojiFormatted = event.getReaction().getEmoji().getFormatted();
        Optional<ReactionRoleEntry> entryOpt = rrMessageOpt.get().getEntries().stream()
                .filter(entry -> entry.getType() == ReactionRoleType.REACTION)
                .filter(entry -> emojiFormatted.equals(entry.getEmoji()))
                .findFirst();

        entryOpt.ifPresent(entry -> event.retrieveMember().queue(member -> {
            Role role = guild.getRoleById(entry.getRoleId());
            if (role == null) {
                LOGGER.error("Role {} not found for reaction role message {}", entry.getRoleId(), event.getMessageId());
                return;
            }
            RetryingRestAction.queueWithRetry(
                    () -> guild.addRoleToMember(member, role),
                    success -> {
                        LOGGER.info("Assigned role {} to member {} via reaction", role.getId(), member.getId());
                        BotMetrics.incrementReactionRolesAssigned();
                    },
                    failure -> LOGGER.error("Failed to assign role {} to member {}: {}", role.getId(), member.getId(), failure.getMessage()),
                    LOGGER, "Rollenvergabe " + role.getId() + " -> " + member.getId());
        }));
    }

    @Override
    public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
        try {
            handleReactionRemove(event);
        } catch (Exception e) {
            LOGGER.error("Unerwarteter Fehler bei Reaction-Remove auf Nachricht {}: ", event.getMessageId(), e);
        }
    }

    private void handleReactionRemove(MessageReactionRemoveEvent event) {
        if (!event.isFromGuild()) {
            return;
        }

        Guild guild = event.getGuild();
        Optional<ReactionRoleMessage> rrMessageOpt = service.findMessage(guild.getId(), event.getMessageId());
        if (rrMessageOpt.isEmpty()) {
            return;
        }

        String emojiFormatted = event.getReaction().getEmoji().getFormatted();
        Optional<ReactionRoleEntry> entryOpt = rrMessageOpt.get().getEntries().stream()
                .filter(entry -> entry.getType() == ReactionRoleType.REACTION)
                .filter(entry -> emojiFormatted.equals(entry.getEmoji()))
                .findFirst();

        entryOpt.ifPresent(entry -> event.retrieveMember().queue(member -> {
            if (member.getUser().isBot()) {
                return;
            }
            Role role = guild.getRoleById(entry.getRoleId());
            if (role == null) {
                return;
            }
            RetryingRestAction.queueWithRetry(
                    () -> guild.removeRoleFromMember(member, role),
                    success -> LOGGER.info("Removed role {} from member {} via reaction", role.getId(), member.getId()),
                    failure -> LOGGER.error("Failed to remove role {} from member {}: {}", role.getId(), member.getId(), failure.getMessage()),
                    LOGGER, "Rollenentzug " + role.getId() + " -> " + member.getId());
        }));
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        try {
            handleButtonInteraction(event);
        } catch (Exception e) {
            LOGGER.error("Unerwarteter Fehler bei Reaction-Role-Button {} in Guild {}: ",
                    event.getComponentId(), event.getGuild() != null ? event.getGuild().getId() : "unknown", e);
            if (!event.isAcknowledged()) {
                event.reply("Es ist ein unerwarteter Fehler aufgetreten.").setEphemeral(true).queue();
            }
        }
    }

    private void handleButtonInteraction(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        if (!componentId.startsWith("reactionrole:")) {
            return;
        }

        String[] parts = componentId.split(":");
        if (parts.length != 3) {
            return;
        }

        Guild guild = event.getGuild();
        Member member = event.getMember();
        if (guild == null || member == null) {
            return;
        }

        Role role = guild.getRoleById(parts[2]);
        if (role == null) {
            event.reply("Diese Rolle existiert nicht mehr.").setEphemeral(true).queue();
            return;
        }

        if (member.getRoles().contains(role)) {
            RetryingRestAction.queueWithRetry(
                    () -> guild.removeRoleFromMember(member, role),
                    success -> event.reply("Rolle **" + role.getName() + "** wurde entfernt.").setEphemeral(true).queue(),
                    failure -> {
                        LOGGER.error("Failed to remove role {} from member {}: {}", role.getId(), member.getId(), failure.getMessage());
                        event.reply("Die Rolle konnte nicht entfernt werden.").setEphemeral(true).queue();
                    },
                    LOGGER, "Rollenentzug (Button) " + role.getId() + " -> " + member.getId());
        } else {
            RetryingRestAction.queueWithRetry(
                    () -> guild.addRoleToMember(member, role),
                    success -> event.reply("Rolle **" + role.getName() + "** wurde vergeben.").setEphemeral(true).queue(),
                    failure -> {
                        LOGGER.error("Failed to add role {} to member {}: {}", role.getId(), member.getId(), failure.getMessage());
                        event.reply("Die Rolle konnte nicht vergeben werden.").setEphemeral(true).queue();
                    },
                    LOGGER, "Rollenvergabe (Button) " + role.getId() + " -> " + member.getId());
        }
    }
}
