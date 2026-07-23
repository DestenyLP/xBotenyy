package de.destenylp.xBotenyy.discordbot.listeners;

import de.destenylp.xBotenyy.discordbot.observability.BotMetrics;
import de.destenylp.xBotenyy.discordbot.tickets.Ticket;
import de.destenylp.xBotenyy.discordbot.tickets.TicketCategory;
import de.destenylp.xBotenyy.discordbot.tickets.TicketCloseCoordinator;
import de.destenylp.xBotenyy.discordbot.tickets.TicketEmbedFactory;
import de.destenylp.xBotenyy.discordbot.tickets.TicketPriority;
import de.destenylp.xBotenyy.discordbot.tickets.TicketService;
import de.destenylp.xBotenyy.discordbot.tickets.TicketSettings;
import de.destenylp.xBotenyy.common.util.AuditLog;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Optional;

public class TicketListener extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TicketListener.class);

    private final TicketService service;
    private final TicketCloseCoordinator closeCoordinator;
    private final int subjectMaxLength;
    private final int descriptionMaxLength;
    private final int closeReasonMaxLength;

    public TicketListener(TicketService service, TicketCloseCoordinator closeCoordinator,
                           int subjectMaxLength, int descriptionMaxLength, int closeReasonMaxLength) {
        this.service = service;
        this.closeCoordinator = closeCoordinator;
        this.subjectMaxLength = subjectMaxLength;
        this.descriptionMaxLength = descriptionMaxLength;
        this.closeReasonMaxLength = closeReasonMaxLength;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.getAuthor().isBot()) {
            return;
        }
        try {
            service.recordActivity(event.getGuild().getId(), event.getChannel().getId());
        } catch (Exception e) {
            LOGGER.error("Unerwarteter Fehler beim Erfassen der Ticket-Aktivitaet (Guild: {}, Channel: {}): ",
                    event.getGuild().getId(), event.getChannel().getId(), e);
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        try {
            handleStringSelect(event);
        } catch (Exception e) {
            LOGGER.error("Unerwarteter Fehler bei Ticket-Select-Interaktion {}: ", event.getComponentId(), e);
            replyGenericError(event);
        }
    }

    private void handleStringSelect(StringSelectInteractionEvent event) {
        String componentId = event.getComponentId();
        if ("ticket:panel:category".equals(componentId)) {
            handleCategorySelect(event);
        } else if (componentId.startsWith("ticket:priority:")) {
            handlePrioritySelect(event, componentId.substring("ticket:priority:".length()));
        }
    }

    private void handleCategorySelect(StringSelectInteractionEvent event) {
        String key = event.getValues().isEmpty() ? null : event.getValues().get(0);
        Optional<TicketCategory> categoryOpt = TicketCategory.fromKey(key);
        if (categoryOpt.isEmpty()) {
            event.reply("Ungültige Kategorie.").setEphemeral(true).queue();
            return;
        }
        TicketCategory category = categoryOpt.get();

        TextInput subject = TextInput.create("subject", TextInputStyle.SHORT)
                .setPlaceholder("Kurze Zusammenfassung deines Anliegens")
                .setMaxLength(subjectMaxLength)
                .build();

        TextInput description = TextInput.create("description", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Beschreibe dein Anliegen so genau wie möglich...")
                .setRequired(false)
                .setMaxLength(descriptionMaxLength)
                .build();

        Modal modal = Modal.create("ticket:createmodal:" + category.getKey(), "Neues Ticket: " + category.getLabel())
                .addComponents(
                        Label.of("Betreff", subject),
                        Label.of("Beschreibung (optional)", description))
                .build();

        event.replyModal(modal).queue();
    }

    private void handlePrioritySelect(StringSelectInteractionEvent event, String ticketId) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        if (guild == null || member == null) {
            return;
        }

        TicketSettings settings = service.getSettings(guild.getId()).orElse(null);
        if (!TicketService.isStaff(member, settings)) {
            event.reply("Dir fehlt die Berechtigung, die Priorität zu ändern.").setEphemeral(true).queue();
            return;
        }

        String key = event.getValues().isEmpty() ? null : event.getValues().get(0);
        Optional<TicketPriority> priorityOpt = TicketPriority.fromKey(key);
        if (priorityOpt.isEmpty()) {
            event.reply("Ungültige Priorität.").setEphemeral(true).queue();
            return;
        }

        boolean success = service.setPriority(guild.getId(), ticketId, priorityOpt.get());
        if (!success) {
            event.reply("Priorität konnte nicht geändert werden.").setEphemeral(true).queue();
            return;
        }

        Ticket ticket = service.getTicket(guild.getId(), ticketId).orElseThrow();
        event.editMessageEmbeds(TicketEmbedFactory.buildTicketEmbed(ticket))
                .setComponents(TicketEmbedFactory.buildTicketComponents(ticket))
                .queue();
        AuditLog.record(guild.getId(), member.getId(), "TICKET_PRIORITY", "ticketId=" + ticketId + " priority=" + priorityOpt.get().getKey());
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        try {
            handleModal(event);
        } catch (Exception e) {
            LOGGER.error("Unerwarteter Fehler bei Ticket-Modal-Interaktion {}: ", event.getModalId(), e);
            replyGenericError(event);
        }
    }

    private void handleModal(ModalInteractionEvent event) {
        String modalId = event.getModalId();
        if (modalId.startsWith("ticket:createmodal:")) {
            handleCreateSubmission(event, modalId.substring("ticket:createmodal:".length()));
        } else if (modalId.startsWith("ticket:closemodal:")) {
            handleCloseSubmission(event, modalId.substring("ticket:closemodal:".length()));
        }
    }

    private void handleCreateSubmission(ModalInteractionEvent event, String categoryKey) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        if (guild == null || member == null) {
            event.reply("Dieser Vorgang ist nur auf einem Server möglich.").setEphemeral(true).queue();
            return;
        }

        Optional<TicketCategory> categoryOpt = TicketCategory.fromKey(categoryKey);
        if (categoryOpt.isEmpty()) {
            event.reply("Ungültige Kategorie.").setEphemeral(true).queue();
            return;
        }

        TicketService.CreateEligibility eligibility = service.checkEligibility(guild.getId(), member.getId());
        if (eligibility == TicketService.CreateEligibility.NOT_CONFIGURED) {
            event.reply("Das Ticket-System wurde auf diesem Server noch nicht eingerichtet.").setEphemeral(true).queue();
            return;
        }
        if (eligibility == TicketService.CreateEligibility.LIMIT_REACHED) {
            event.reply("Du hast bereits die maximale Anzahl offener Tickets erreicht. Bitte schließe zunächst ein bestehendes Ticket.")
                    .setEphemeral(true).queue();
            return;
        }

        TicketSettings settings = service.getSettings(guild.getId()).orElseThrow();
        Category discordCategory = guild.getCategoryById(settings.getCategoryChannelId());
        if (discordCategory == null) {
            event.reply("Die konfigurierte Ticket-Kategorie existiert nicht mehr. Bitte informiere das Team.").setEphemeral(true).queue();
            return;
        }

        String subject = getValue(event, "subject");
        String description = getValue(event, "description");

        Ticket ticket = service.createTicket(guild.getId(), member.getId(), member.getUser().getName(),
                categoryOpt.get(), subject, description);

        event.deferReply(true).queue();

        String channelName = "ticket-" + ticket.getId();
        discordCategory.createTextChannel(channelName)
                .addPermissionOverride(guild.getPublicRole(), EnumSet.noneOf(Permission.class), EnumSet.of(Permission.VIEW_CHANNEL))
                .addMemberPermissionOverride(member.getIdLong(),
                        EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY), EnumSet.noneOf(Permission.class))
                .addRolePermissionOverride(Long.parseLong(settings.getSupportRoleId()),
                        EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY), EnumSet.noneOf(Permission.class))
                .queue(channel -> {
                    service.attachChannel(guild.getId(), ticket.getId(), channel.getId());
                    String mention = "<@" + member.getId() + "> <@&" + settings.getSupportRoleId() + ">";
                    channel.sendMessage(mention)
                            .setEmbeds(TicketEmbedFactory.buildTicketEmbed(ticket))
                            .setComponents(TicketEmbedFactory.buildTicketComponents(ticket))
                            .queue(message -> {
                                service.attachControlMessage(guild.getId(), ticket.getId(), message.getId());
                                event.getHook().sendMessage("Dein Ticket wurde erstellt: " + channel.getAsMention())
                                        .setEphemeral(true).queue();
                                LOGGER.info("Ticket {} created by {} in guild {} (channel {})",
                                        ticket.getId(), member.getId(), guild.getId(), channel.getId());
                                AuditLog.record(guild.getId(), member.getId(), "TICKET_CREATE", "ticketId=" + ticket.getId());
                                BotMetrics.incrementTicketsCreated();
                            });
                }, failure -> {
                    LOGGER.error("Ticket-Kanal für Ticket {} konnte nicht erstellt werden: {}", ticket.getId(), failure.getMessage());
                    event.getHook().sendMessage("Dein Ticket wurde gespeichert, der Kanal konnte aber nicht erstellt werden. Bitte informiere einen Admin.")
                            .setEphemeral(true).queue();
                });
    }

    private void handleCloseSubmission(ModalInteractionEvent event, String ticketId) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        if (guild == null || member == null) {
            return;
        }

        String reason = getValue(event, "reason");
        boolean success = service.close(guild.getId(), ticketId, member.getId(), member.getUser().getName(), reason);
        if (!success) {
            event.reply("Dieses Ticket ist bereits geschlossen.").setEphemeral(true).queue();
            return;
        }

        String closeConfirmation = "Ticket `" + ticketId + "` wird geschlossen...";
        TicketSettings settings = service.getSettings(guild.getId()).orElse(null);
        if (settings == null || settings.getLogChannelId() == null) {
            closeConfirmation += "\n⚠️ Kein Log-Kanal konfiguriert (`/ticket settings logchannel:#kanal`) – dieses Ticket wird nirgends protokolliert!";
        }
        event.reply(closeConfirmation).queue();
        LOGGER.info("Ticket {} closed by {} in guild {}", ticketId, member.getId(), guild.getId());
        AuditLog.record(guild.getId(), member.getId(), "TICKET_CLOSE", "ticketId=" + ticketId + " reason=" + reason);

        Ticket closedTicket = service.getTicket(guild.getId(), ticketId).orElseThrow();
        closeCoordinator.finalizeClose(event.getChannel().asTextChannel(), closedTicket);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        try {
            handleButton(event);
        } catch (Exception e) {
            LOGGER.error("Unerwarteter Fehler bei Ticket-Button-Interaktion {}: ", event.getComponentId(), e);
            replyGenericError(event);
        }
    }

    private void handleButton(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        if (!componentId.startsWith("ticket:")) {
            return;
        }

        String[] parts = componentId.split(":");
        if (parts.length < 3) {
            return;
        }
        String action = parts[1];

        if ("rate".equals(action) && parts.length == 5) {
            handleRating(event, parts[2], parts[3], Integer.parseInt(parts[4]));
            return;
        }

        Guild guild = event.getGuild();
        Member member = event.getMember();
        if (guild == null || member == null) {
            return;
        }
        String ticketId = parts[2];

        TicketSettings settings = service.getSettings(guild.getId()).orElse(null);
        Optional<Ticket> ticketOpt = service.getTicket(guild.getId(), ticketId);
        if (ticketOpt.isEmpty()) {
            event.reply("Dieses Ticket existiert nicht mehr.").setEphemeral(true).queue();
            return;
        }
        Ticket ticket = ticketOpt.get();

        switch (action) {
            case "claim" -> handleClaim(event, guild, member, settings, ticket);
            case "unclaim" -> handleUnclaim(event, guild, member, settings, ticket);
            case "close" -> handleClosePrompt(event, guild, member, settings, ticket);
            default -> {
            }
        }
    }

    private void handleClaim(ButtonInteractionEvent event, Guild guild, Member member, TicketSettings settings, Ticket ticket) {
        if (!TicketService.isStaff(member, settings)) {
            event.reply("Nur das Support-Team kann Tickets übernehmen.").setEphemeral(true).queue();
            return;
        }
        if (ticket.getStatus().isClosed()) {
            event.reply("Dieses Ticket ist bereits geschlossen.").setEphemeral(true).queue();
            return;
        }

        boolean success = service.claim(guild.getId(), ticket.getId(), member.getId(), member.getUser().getName());
        if (!success) {
            event.reply("Dieses Ticket konnte nicht übernommen werden.").setEphemeral(true).queue();
            return;
        }

        Ticket updated = service.getTicket(guild.getId(), ticket.getId()).orElseThrow();
        event.editMessageEmbeds(TicketEmbedFactory.buildTicketEmbed(updated))
                .setComponents(TicketEmbedFactory.buildTicketComponents(updated))
                .queue();
        LOGGER.info("Ticket {} claimed by {} in guild {}", ticket.getId(), member.getId(), guild.getId());
        AuditLog.record(guild.getId(), member.getId(), "TICKET_CLAIM", "ticketId=" + ticket.getId());
    }

    private void handleUnclaim(ButtonInteractionEvent event, Guild guild, Member member, TicketSettings settings, Ticket ticket) {
        if (!TicketService.isStaff(member, settings)) {
            event.reply("Nur das Support-Team kann Tickets freigeben.").setEphemeral(true).queue();
            return;
        }
        if (ticket.getStatus().isClosed()) {
            event.reply("Dieses Ticket ist bereits geschlossen.").setEphemeral(true).queue();
            return;
        }

        boolean success = service.unclaim(guild.getId(), ticket.getId());
        if (!success) {
            event.reply("Dieses Ticket konnte nicht freigegeben werden.").setEphemeral(true).queue();
            return;
        }

        Ticket updated = service.getTicket(guild.getId(), ticket.getId()).orElseThrow();
        event.editMessageEmbeds(TicketEmbedFactory.buildTicketEmbed(updated))
                .setComponents(TicketEmbedFactory.buildTicketComponents(updated))
                .queue();
        LOGGER.info("Ticket {} unclaimed by {} in guild {}", ticket.getId(), member.getId(), guild.getId());
        AuditLog.record(guild.getId(), member.getId(), "TICKET_UNCLAIM", "ticketId=" + ticket.getId());
    }

    private void handleClosePrompt(ButtonInteractionEvent event, Guild guild, Member member, TicketSettings settings, Ticket ticket) {
        boolean isOwner = ticket.getAuthorId().equals(member.getId());
        if (!isOwner && !TicketService.isStaff(member, settings)) {
            event.reply("Nur das Team oder der Ersteller können dieses Ticket schließen.").setEphemeral(true).queue();
            return;
        }
        if (ticket.getStatus().isClosed()) {
            event.reply("Dieses Ticket ist bereits geschlossen.").setEphemeral(true).queue();
            return;
        }

        TextInput reason = TextInput.create("reason", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Warum wird dieses Ticket geschlossen? (optional)")
                .setRequired(false)
                .setMaxLength(closeReasonMaxLength)
                .build();

        Modal modal = Modal.create("ticket:closemodal:" + ticket.getId(), "Ticket schließen")
                .addComponents(Label.of("Grund (optional)", reason))
                .build();

        event.replyModal(modal).queue();
    }

    private void handleRating(ButtonInteractionEvent event, String guildId, String ticketId, int score) {
        boolean success = service.rate(guildId, ticketId, score, null);
        if (!success) {
            event.reply("Diese Bewertung konnte nicht gespeichert werden.").setEphemeral(true).queue();
            return;
        }
        event.editMessage("Danke für deine Bewertung! " + "\u2B50".repeat(score)).setComponents().queue(
                success2 -> { }, failure -> { });
        service.getTicket(guildId, ticketId).ifPresent(ticket -> closeCoordinator.refreshLogMessage(event.getJDA(), ticket));
        LOGGER.info("Ticket {} rated {}/5 by {}", ticketId, score, event.getUser().getId());
        AuditLog.record(guildId, event.getUser().getId(), "TICKET_RATE", "ticketId=" + ticketId + " score=" + score);
    }

    private String getValue(ModalInteractionEvent event, String id) {
        ModalMapping mapping = event.getValue(id);
        return mapping != null ? mapping.getAsString() : null;
    }

    private void replyGenericError(IReplyCallback event) {
        if (event.isAcknowledged()) {
            event.getHook().sendMessage("Es ist ein unerwarteter Fehler aufgetreten. Bitte versuche es erneut.").queue();
        } else {
            event.reply("Es ist ein unerwarteter Fehler aufgetreten. Bitte versuche es erneut.").setEphemeral(true).queue();
        }
    }
}
