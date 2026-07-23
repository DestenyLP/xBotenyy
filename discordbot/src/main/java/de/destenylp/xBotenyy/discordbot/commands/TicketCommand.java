package de.destenylp.xBotenyy.discordbot.commands;

import de.destenylp.xBotenyy.discordbot.core.AbstractGuildCommand;
import de.destenylp.xBotenyy.discordbot.tickets.Ticket;
import de.destenylp.xBotenyy.discordbot.tickets.TicketCloseCoordinator;
import de.destenylp.xBotenyy.discordbot.tickets.TicketEmbedFactory;
import de.destenylp.xBotenyy.discordbot.tickets.TicketPriority;
import de.destenylp.xBotenyy.discordbot.tickets.TicketService;
import de.destenylp.xBotenyy.discordbot.tickets.TicketSettings;
import de.destenylp.xBotenyy.common.util.AuditLog;
import de.destenylp.xBotenyy.discordbot.util.PermissionGuard;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public class TicketCommand extends AbstractGuildCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(TicketCommand.class);

    private final TicketService service;
    private final TicketCloseCoordinator closeCoordinator;

    public TicketCommand(TicketService service, TicketCloseCoordinator closeCoordinator) {
        this.service = service;
        this.closeCoordinator = closeCoordinator;
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("ticket", "Verwaltet das Support-Ticket-System")
                .addSubcommands(
                        new SubcommandData("panel", "Postet das Panel zum Eröffnen neuer Tickets in diesem Kanal (nur Team)"),
                        new SubcommandData("settings", "Konfiguriert das Ticket-System (nur Team, ohne Optionen: zeigt aktuellen Stand)")
                                .addOptions(new OptionData(OptionType.CHANNEL, "category", "Discord-Kategorie, unter der Ticket-Kanäle erstellt werden", false)
                                        .setChannelTypes(ChannelType.CATEGORY))
                                .addOption(OptionType.ROLE, "supportrole", "Rolle, die Tickets sehen und bearbeiten darf", false)
                                .addOption(OptionType.CHANNEL, "logchannel", "Kanal, in dem geschlossene Tickets protokolliert werden", false)
                                .addOption(OptionType.CHANNEL, "transcriptchannel", "Kanal für Transcript-Uploads (optional, sonst logchannel)", false)
                                .addOption(OptionType.INTEGER, "maxopen", "Maximale Anzahl gleichzeitig offener Tickets pro Mitglied", false)
                                .addOption(OptionType.INTEGER, "autoclosehours", "Stunden ohne Aktivität, bevor ein Ticket automatisch geschlossen wird (0 = deaktiviert)", false),
                        new SubcommandData("list", "Zeigt alle aktuell offenen Tickets (nur Team)"),
                        new SubcommandData("claim", "Übernimmt das Ticket in diesem Kanal (nur Team)"),
                        new SubcommandData("unclaim", "Gibt das Ticket in diesem Kanal wieder frei (nur Team)"),
                        new SubcommandData("close", "Schließt das Ticket in diesem Kanal")
                                .addOption(OptionType.STRING, "reason", "Grund für das Schließen", false),
                        new SubcommandData("priority", "Setzt die Priorität des Tickets in diesem Kanal (nur Team)")
                                .addOptions(new OptionData(OptionType.STRING, "level", "Neue Priorität", true)
                                        .addChoice("Niedrig", TicketPriority.LOW.getKey())
                                        .addChoice("Mittel", TicketPriority.MEDIUM.getKey())
                                        .addChoice("Hoch", TicketPriority.HIGH.getKey())
                                        .addChoice("Dringend", TicketPriority.URGENT.getKey())),
                        new SubcommandData("add", "Fügt ein Mitglied zu diesem Ticket hinzu (nur Team)")
                                .addOption(OptionType.USER, "member", "Mitglied, das hinzugefügt werden soll", true),
                        new SubcommandData("remove", "Entfernt ein Mitglied aus diesem Ticket (nur Team)")
                                .addOption(OptionType.USER, "member", "Mitglied, das entfernt werden soll", true)
                );
    }

    @Override
    protected void executeInGuild(SlashCommandInteractionEvent event, Guild guild, String subcommand) {
        switch (subcommand) {
            case "panel" -> handlePanel(event);
            case "settings" -> handleSettings(event);
            case "list" -> handleList(event);
            case "claim" -> handleClaim(event);
            case "unclaim" -> handleUnclaim(event);
            case "close" -> handleClose(event);
            case "priority" -> handlePriority(event);
            case "add" -> handleAdd(event);
            case "remove" -> handleRemove(event);
            default -> replyUnknownSubcommand(event);
        }
    }

    private void handlePanel(SlashCommandInteractionEvent event) {
        if (!PermissionGuard.requireManageServer(event)) {
            return;
        }
        Guild guild = event.getGuild();
        TicketSettings settings = service.getSettings(guild.getId()).orElse(null);
        if (settings == null || !settings.isConfigured()) {
            event.reply("Bitte konfiguriere zuerst `/ticket settings`, bevor du das Panel postest.").setEphemeral(true).queue();
            return;
        }

        TextChannel channel = event.getChannel().asTextChannel();
        channel.sendMessageEmbeds(TicketEmbedFactory.buildPanelEmbed())
                .setComponents(TicketEmbedFactory.buildPanelComponents())
                .queue(message -> {
                    service.updatePanel(guild.getId(), channel.getId(), message.getId());
                    event.reply("Ticket-Panel wurde gepostet!").setEphemeral(true).queue();
                    LOGGER.info("Ticket panel posted in guild {} channel {}", guild.getId(), channel.getId());
                }, failure -> event.reply("Das Panel konnte nicht gepostet werden: " + failure.getMessage()).setEphemeral(true).queue());
    }

    private void handleSettings(SlashCommandInteractionEvent event) {
        if (!PermissionGuard.requireManageServer(event)) {
            return;
        }
        Guild guild = event.getGuild();
        OptionMapping categoryOpt = event.getOption("category");
        OptionMapping roleOpt = event.getOption("supportrole");
        OptionMapping logOpt = event.getOption("logchannel");
        OptionMapping transcriptOpt = event.getOption("transcriptchannel");
        OptionMapping maxOpenOpt = event.getOption("maxopen");
        OptionMapping autoCloseOpt = event.getOption("autoclosehours");

        boolean noOptionsGiven = categoryOpt == null && roleOpt == null && logOpt == null
                && transcriptOpt == null && maxOpenOpt == null && autoCloseOpt == null;

        if (noOptionsGiven) {
            event.reply(describeSettings(service.getSettings(guild.getId()).orElse(null))).setEphemeral(true).queue();
            return;
        }

        if (categoryOpt != null) {
            var discordCategory = categoryOpt.getAsChannel().asCategory();
            service.updateCategoryChannel(guild.getId(), discordCategory.getId());
            discordCategory.upsertPermissionOverride(guild.getPublicRole())
                    .setDenied(EnumSet.of(Permission.VIEW_CHANNEL))
                    .queue();
        }
        if (roleOpt != null) {
            service.updateSupportRole(guild.getId(), roleOpt.getAsRole().getId());
        }
        if (logOpt != null) {
            service.updateLogChannel(guild.getId(), logOpt.getAsChannel().getId());
        }
        if (transcriptOpt != null) {
            service.updateTranscriptChannel(guild.getId(), transcriptOpt.getAsChannel().getId());
        }
        if (maxOpenOpt != null) {
            service.updateMaxOpenTicketsPerMember(guild.getId(), (int) maxOpenOpt.getAsLong());
        }
        if (autoCloseOpt != null) {
            service.updateAutoCloseInactivityHours(guild.getId(), (int) autoCloseOpt.getAsLong());
        }

        TicketSettings updated = service.getSettings(guild.getId()).orElse(null);
        event.reply("Einstellungen aktualisiert!\n" + describeSettings(updated)).setEphemeral(true).queue();
        LOGGER.info("Ticket settings updated for guild {}", guild.getId());
        AuditLog.record(guild.getId(), event.getUser().getId(), "TICKET_SETTINGS_UPDATE", "settings=" + describeSettings(updated));
    }

    private String describeSettings(TicketSettings settings) {
        if (settings == null || !settings.isConfigured()) {
            return "Noch nicht vollständig konfiguriert. Nutze `/ticket settings category:<Kategorie> supportrole:@Team`.";
        }
        return "Kategorie: <#" + settings.getCategoryChannelId() + ">\n" +
                "Support-Rolle: <@&" + settings.getSupportRoleId() + ">\n" +
                "Log-Kanal: " + (settings.getLogChannelId() != null ? "<#" + settings.getLogChannelId() + ">" : "Nicht gesetzt – geschlossene Tickets werden nirgends protokolliert!") + "\n" +
                "Transcript-Kanal: " + (settings.getTranscriptChannelId() != null ? "<#" + settings.getTranscriptChannelId() + "> (zusätzlich zum Log-Kanal)" : "Nicht gesetzt – Transcript wird an den Log-Kanal angehängt") + "\n" +
                "Max. offene Tickets/Mitglied: " + settings.getMaxOpenTicketsPerMember() + "\n" +
                "Auto-Close nach Inaktivität: " + (settings.getAutoCloseInactivityHours() > 0 ? settings.getAutoCloseInactivityHours() + "h" : "Deaktiviert") + "\n" +
                "Tickets gesamt: " + settings.getTickets().size();
    }

    private void handleList(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        TicketSettings settings = service.getSettings(guild.getId()).orElse(null);
        if (!requireStaff(event, settings)) {
            return;
        }
        List<Ticket> open = service.getOpenTickets(guild.getId());
        event.replyEmbeds(TicketEmbedFactory.buildOverviewEmbed(guild.getName(), open)).setEphemeral(true).queue();
    }

    private void handleClaim(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        TicketSettings settings = service.getSettings(guild.getId()).orElse(null);
        if (!requireStaff(event, settings)) {
            return;
        }
        Optional<Ticket> ticketOpt = requireTicketChannel(event, guild.getId());
        if (ticketOpt.isEmpty()) {
            return;
        }
        Ticket ticket = ticketOpt.get();
        boolean success = service.claim(guild.getId(), ticket.getId(), member.getId(), member.getUser().getName());
        if (!success) {
            event.reply("Dieses Ticket konnte nicht übernommen werden.").setEphemeral(true).queue();
            return;
        }
        Ticket updated = service.getTicket(guild.getId(), ticket.getId()).orElseThrow();
        refreshTicketMessage(event, updated);
        event.reply("Du hast das Ticket übernommen.").setEphemeral(true).queue();
        LOGGER.info("Ticket {} claimed by {} in guild {}", ticket.getId(), member.getId(), guild.getId());
        AuditLog.record(guild.getId(), member.getId(), "TICKET_CLAIM", "ticketId=" + ticket.getId());
    }

    private void handleUnclaim(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        TicketSettings settings = service.getSettings(guild.getId()).orElse(null);
        if (!requireStaff(event, settings)) {
            return;
        }
        Optional<Ticket> ticketOpt = requireTicketChannel(event, guild.getId());
        if (ticketOpt.isEmpty()) {
            return;
        }
        Ticket ticket = ticketOpt.get();
        boolean success = service.unclaim(guild.getId(), ticket.getId());
        if (!success) {
            event.reply("Dieses Ticket konnte nicht freigegeben werden.").setEphemeral(true).queue();
            return;
        }
        Ticket updated = service.getTicket(guild.getId(), ticket.getId()).orElseThrow();
        refreshTicketMessage(event, updated);
        event.reply("Ticket wurde freigegeben.").setEphemeral(true).queue();
        LOGGER.info("Ticket {} unclaimed by {} in guild {}", ticket.getId(), member.getId(), guild.getId());
        AuditLog.record(guild.getId(), member.getId(), "TICKET_UNCLAIM", "ticketId=" + ticket.getId());
    }

    private void handleClose(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        TicketSettings settings = service.getSettings(guild.getId()).orElse(null);
        Optional<Ticket> ticketOpt = requireTicketChannel(event, guild.getId());
        if (ticketOpt.isEmpty()) {
            return;
        }
        Ticket ticket = ticketOpt.get();
        boolean isOwner = ticket.getAuthorId().equals(member.getId());
        if (!isOwner && !TicketService.isStaff(member, settings)) {
            event.reply("Nur das Team oder der Ersteller können dieses Ticket schließen.").setEphemeral(true).queue();
            return;
        }

        OptionMapping reasonOpt = event.getOption("reason");
        String reason = reasonOpt != null ? reasonOpt.getAsString() : null;

        boolean success = service.close(guild.getId(), ticket.getId(), member.getId(), member.getUser().getName(), reason);
        if (!success) {
            event.reply("Dieses Ticket ist bereits geschlossen.").setEphemeral(true).queue();
            return;
        }

        String closeConfirmation = "Ticket `" + ticket.getId() + "` wird geschlossen...";
        if (settings == null || settings.getLogChannelId() == null) {
            closeConfirmation += "\n⚠️ Kein Log-Kanal konfiguriert (`/ticket settings logchannel:#kanal`) – dieses Ticket wird nirgends protokolliert!";
        }
        event.reply(closeConfirmation).queue();
        LOGGER.info("Ticket {} closed by {} in guild {}", ticket.getId(), member.getId(), guild.getId());
        AuditLog.record(guild.getId(), member.getId(), "TICKET_CLOSE", "ticketId=" + ticket.getId() + " reason=" + reason);

        Ticket closedTicket = service.getTicket(guild.getId(), ticket.getId()).orElseThrow();
        closeCoordinator.finalizeClose(event.getChannel().asTextChannel(), closedTicket);
    }

    private void handlePriority(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        TicketSettings settings = service.getSettings(guild.getId()).orElse(null);
        if (!requireStaff(event, settings)) {
            return;
        }
        Optional<Ticket> ticketOpt = requireTicketChannel(event, guild.getId());
        if (ticketOpt.isEmpty()) {
            return;
        }
        Ticket ticket = ticketOpt.get();

        String level = event.getOption("level").getAsString();
        Optional<TicketPriority> priorityOpt = TicketPriority.fromKey(level);
        if (priorityOpt.isEmpty()) {
            event.reply("Ungültige Priorität.").setEphemeral(true).queue();
            return;
        }

        service.setPriority(guild.getId(), ticket.getId(), priorityOpt.get());
        Ticket updated = service.getTicket(guild.getId(), ticket.getId()).orElseThrow();
        refreshTicketMessage(event, updated);
        event.reply("Priorität wurde auf **" + priorityOpt.get().getLabel() + "** gesetzt.").setEphemeral(true).queue();
        AuditLog.record(guild.getId(), member.getId(), "TICKET_PRIORITY", "ticketId=" + ticket.getId() + " priority=" + priorityOpt.get().getKey());
    }

    private void handleAdd(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        TicketSettings settings = service.getSettings(guild.getId()).orElse(null);
        if (!requireStaff(event, settings)) {
            return;
        }
        Optional<Ticket> ticketOpt = requireTicketChannel(event, guild.getId());
        if (ticketOpt.isEmpty()) {
            return;
        }
        Ticket ticket = ticketOpt.get();
        Member target = event.getOption("member").getAsMember();
        if (target == null) {
            event.reply("Dieses Mitglied konnte nicht gefunden werden.").setEphemeral(true).queue();
            return;
        }

        boolean added = service.addParticipant(guild.getId(), ticket.getId(), target.getId());
        if (!added) {
            event.reply("Dieses Mitglied ist bereits Teil des Tickets.").setEphemeral(true).queue();
            return;
        }

        TextChannel channel = event.getChannel().asTextChannel();
        channel.upsertPermissionOverride(target)
                .setAllowed(EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY))
                .queue();
        event.reply("<@" + target.getId() + "> wurde zum Ticket hinzugefügt.").queue();
        AuditLog.record(guild.getId(), member.getId(), "TICKET_ADD_MEMBER", "ticketId=" + ticket.getId() + " member=" + target.getId());
    }

    private void handleRemove(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        TicketSettings settings = service.getSettings(guild.getId()).orElse(null);
        if (!requireStaff(event, settings)) {
            return;
        }
        Optional<Ticket> ticketOpt = requireTicketChannel(event, guild.getId());
        if (ticketOpt.isEmpty()) {
            return;
        }
        Ticket ticket = ticketOpt.get();
        Member target = event.getOption("member").getAsMember();
        if (target == null) {
            event.reply("Dieses Mitglied konnte nicht gefunden werden.").setEphemeral(true).queue();
            return;
        }

        boolean removed = service.removeParticipant(guild.getId(), ticket.getId(), target.getId());
        if (!removed) {
            event.reply("Dieses Mitglied ist nicht Teil des Tickets.").setEphemeral(true).queue();
            return;
        }

        TextChannel channel = event.getChannel().asTextChannel();
        channel.upsertPermissionOverride(target).setDenied(EnumSet.of(Permission.VIEW_CHANNEL)).queue();
        event.reply("<@" + target.getId() + "> wurde aus dem Ticket entfernt.").queue();
        AuditLog.record(guild.getId(), member.getId(), "TICKET_REMOVE_MEMBER", "ticketId=" + ticket.getId() + " member=" + target.getId());
    }

    private boolean requireStaff(SlashCommandInteractionEvent event, TicketSettings settings) {
        Member member = event.getMember();
        if (member == null || !TicketService.isStaff(member, settings)) {
            event.reply("Dir fehlt die Berechtigung, diesen Befehl zu nutzen.").setEphemeral(true).queue();
            return false;
        }
        return true;
    }

    private Optional<Ticket> requireTicketChannel(SlashCommandInteractionEvent event, String guildId) {
        Optional<Ticket> ticketOpt = service.getTicketByChannel(guildId, event.getChannel().getId());
        if (ticketOpt.isEmpty()) {
            event.reply("Dieser Befehl kann nur in einem Ticket-Kanal genutzt werden.").setEphemeral(true).queue();
        }
        return ticketOpt;
    }

    private void refreshTicketMessage(SlashCommandInteractionEvent event, Ticket ticket) {
        if (ticket.getControlMessageId() == null) {
            return;
        }
        TextChannel channel = event.getChannel().asTextChannel();
        channel.editMessageEmbedsById(ticket.getControlMessageId(), TicketEmbedFactory.buildTicketEmbed(ticket))
                .setComponents(TicketEmbedFactory.buildTicketComponents(ticket))
                .queue(success -> { }, failure -> LOGGER.warn("Konnte Ticket-Nachricht {} nicht aktualisieren: {}",
                        ticket.getControlMessageId(), failure.getMessage()));
    }
}
