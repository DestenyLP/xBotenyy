package de.destenylp.xBotenyy.discordbot.tickets;

import de.destenylp.xBotenyy.discordbot.core.PrunableGuildService;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TicketService implements PrunableGuildService {
    private final TicketRepository manager;
    private final int gracePeriodDivisor;
    private final int gracePeriodMinHours;

    public TicketService(TicketRepository manager) {
        this(manager, 2, 1);
    }

    public TicketService(TicketRepository manager, int gracePeriodDivisor, int gracePeriodMinHours) {
        this.manager = manager;
        this.gracePeriodDivisor = Math.max(1, gracePeriodDivisor);
        this.gracePeriodMinHours = Math.max(0, gracePeriodMinHours);
    }

    @Override
    public String getServiceName() {
        return "Tickets";
    }

    @Override
    public int pruneOldEntries(Duration retention) {
        return pruneClosedTickets(retention);
    }

    public Optional<TicketSettings> getSettings(String guildId) {
        return manager.getSettings(guildId);
    }

    public void updateCategoryChannel(String guildId, String channelId) {
        manager.updateCategoryChannel(guildId, channelId);
    }

    public void updateSupportRole(String guildId, String roleId) {
        manager.updateSupportRole(guildId, roleId);
    }

    public void updateLogChannel(String guildId, String channelId) {
        manager.updateLogChannel(guildId, channelId);
    }

    public void updateTranscriptChannel(String guildId, String channelId) {
        manager.updateTranscriptChannel(guildId, channelId);
    }

    public void updateMaxOpenTicketsPerMember(String guildId, int max) {
        manager.updateMaxOpenTicketsPerMember(guildId, max);
    }

    public void updateAutoCloseInactivityHours(String guildId, int hours) {
        manager.updateAutoCloseInactivityHours(guildId, hours);
    }

    public void updatePanel(String guildId, String channelId, String messageId) {
        manager.updatePanel(guildId, channelId, messageId);
    }

    public enum CreateEligibility {
        ALLOWED, NOT_CONFIGURED, LIMIT_REACHED
    }

    public CreateEligibility checkEligibility(String guildId, String memberId) {
        TicketSettings settings = manager.getSettings(guildId).orElse(null);
        if (settings == null || !settings.isConfigured()) {
            return CreateEligibility.NOT_CONFIGURED;
        }
        int open = manager.getOpenTicketsByMember(guildId, memberId).size();
        if (open >= settings.getMaxOpenTicketsPerMember()) {
            return CreateEligibility.LIMIT_REACHED;
        }
        return CreateEligibility.ALLOWED;
    }

    public Ticket createTicket(String guildId, String authorId, String authorName, TicketCategory category,
                                String subject, String description) {
        Ticket draft = new Ticket(guildId, authorId, authorName, category, subject, description);
        return manager.createTicket(draft);
    }

    public void attachChannel(String guildId, String ticketId, String channelId) {
        manager.attachChannel(guildId, ticketId, channelId);
    }

    public void attachControlMessage(String guildId, String ticketId, String messageId) {
        manager.attachControlMessage(guildId, ticketId, messageId);
    }

    public Optional<Ticket> getTicket(String guildId, String id) {
        return manager.getTicket(guildId, id);
    }

    public Optional<Ticket> getTicketByChannel(String guildId, String channelId) {
        return manager.getTicketByChannel(guildId, channelId);
    }

    public Optional<Ticket> getTicketByChannelAnyGuild(String channelId) {
        return manager.getTicketByChannelAnyGuild(channelId);
    }

    public List<Ticket> getTickets(String guildId) {
        return manager.getTickets(guildId);
    }

    public List<Ticket> getOpenTickets(String guildId) {
        return manager.getOpenTickets(guildId);
    }

    public List<Ticket> getTicketsByMember(String guildId, String memberId) {
        return manager.getTicketsByMember(guildId, memberId);
    }

    public boolean claim(String guildId, String ticketId, String modId, String modName) {
        return manager.claim(guildId, ticketId, modId, modName);
    }

    public boolean unclaim(String guildId, String ticketId) {
        return manager.unclaim(guildId, ticketId);
    }

    public boolean setPriority(String guildId, String ticketId, TicketPriority priority) {
        return manager.setPriority(guildId, ticketId, priority);
    }

    public boolean addParticipant(String guildId, String ticketId, String memberId) {
        return manager.addParticipant(guildId, ticketId, memberId);
    }

    public boolean removeParticipant(String guildId, String ticketId, String memberId) {
        return manager.removeParticipant(guildId, ticketId, memberId);
    }

    public boolean close(String guildId, String ticketId, String modId, String modName, String reason) {
        return manager.close(guildId, ticketId, modId, modName, reason);
    }

    public void attachLogMessage(String guildId, String ticketId, String logChannelId, String logMessageId) {
        manager.attachLogMessage(guildId, ticketId, logChannelId, logMessageId);
    }

    public void attachTranscript(String guildId, String ticketId, String transcriptFileName) {
        manager.attachTranscript(guildId, ticketId, transcriptFileName);
    }

    public boolean rate(String guildId, String ticketId, int score, String comment) {
        return manager.rate(guildId, ticketId, score, comment);
    }

    public void recordActivity(String guildId, String channelId) {
        manager.recordActivity(guildId, channelId);
    }

    public Map<String, List<Ticket>> findTicketsDueForAutoCloseWarning() {
        Map<String, List<Ticket>> due = new HashMap<>();
        manager.getAllOpenTicketsByGuild().forEach((guildId, tickets) -> {
            TicketSettings settings = manager.getSettings(guildId).orElse(null);
            if (settings == null || settings.getAutoCloseInactivityHours() <= 0) {
                return;
            }
            Instant threshold = Instant.now().minus(Duration.ofHours(settings.getAutoCloseInactivityHours()));
            List<Ticket> matches = tickets.stream()
                    .filter(t -> !t.isAutoCloseWarningSent())
                    .filter(t -> Instant.ofEpochMilli(t.getLastActivityAt()).isBefore(threshold))
                    .toList();
            if (!matches.isEmpty()) {
                due.put(guildId, matches);
            }
        });
        return due;
    }

    public Map<String, List<Ticket>> findTicketsDueForAutoClose() {
        Map<String, List<Ticket>> due = new HashMap<>();
        manager.getAllOpenTicketsByGuild().forEach((guildId, tickets) -> {
            TicketSettings settings = manager.getSettings(guildId).orElse(null);
            if (settings == null || settings.getAutoCloseInactivityHours() <= 0) {
                return;
            }
            int graceHours = Math.max(gracePeriodMinHours, settings.getAutoCloseInactivityHours() / gracePeriodDivisor);
            Instant threshold = Instant.now().minus(Duration.ofHours((long) settings.getAutoCloseInactivityHours() + graceHours));
            List<Ticket> matches = tickets.stream()
                    .filter(Ticket::isAutoCloseWarningSent)
                    .filter(t -> Instant.ofEpochMilli(t.getLastActivityAt()).isBefore(threshold))
                    .toList();
            if (!matches.isEmpty()) {
                due.put(guildId, matches);
            }
        });
        return due;
    }

    public void markAutoCloseWarned(String guildId, String ticketId) {
        manager.markAutoCloseWarned(guildId, ticketId);
    }

    public int pruneClosedTickets(Duration retention) {
        return manager.pruneClosedTickets(retention);
    }

    public static boolean isStaff(Member member, TicketSettings settings) {
        if (member.hasPermission(Permission.ADMINISTRATOR) || member.hasPermission(Permission.MANAGE_SERVER)) {
            return true;
        }
        if (settings != null && settings.getSupportRoleId() != null) {
            return member.getRoles().stream().anyMatch(role -> role.getId().equals(settings.getSupportRoleId()));
        }
        return false;
    }
}
