package de.destenylp.xBotenyy.discordbot.tickets;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TicketRepository {
    TicketSettings getOrCreateSettings(String guildId);
    Optional<TicketSettings> getSettings(String guildId);
    void updateCategoryChannel(String guildId, String channelId);
    void updateSupportRole(String guildId, String roleId);
    void updateLogChannel(String guildId, String channelId);
    void updateTranscriptChannel(String guildId, String channelId);
    void updateMaxOpenTicketsPerMember(String guildId, int max);
    void updateAutoCloseInactivityHours(String guildId, int hours);
    void updatePanel(String guildId, String channelId, String messageId);
    Ticket createTicket(Ticket draft);
    Optional<Ticket> getTicket(String guildId, String id);
    Optional<Ticket> getTicketByChannel(String guildId, String channelId);
    Optional<Ticket> getTicketByChannelAnyGuild(String channelId);
    List<Ticket> getTickets(String guildId);
    List<Ticket> getOpenTickets(String guildId);
    List<Ticket> getOpenTicketsByMember(String guildId, String memberId);
    List<Ticket> getTicketsByMember(String guildId, String memberId);
    Map<String, List<Ticket>> getAllOpenTicketsByGuild();
    int pruneClosedTickets(Duration retention);
    boolean claim(String guildId, String ticketId, String modId, String modName);
    boolean unclaim(String guildId, String ticketId);
    boolean setPriority(String guildId, String ticketId, TicketPriority priority);
    boolean close(String guildId, String ticketId, String modId, String modName, String reason);
    boolean rate(String guildId, String ticketId, int score, String comment);
    boolean addParticipant(String guildId, String ticketId, String memberId);
    boolean removeParticipant(String guildId, String ticketId, String memberId);
    void attachChannel(String guildId, String ticketId, String channelId);
    void attachControlMessage(String guildId, String ticketId, String messageId);
    void attachLogMessage(String guildId, String ticketId, String logChannelId, String logMessageId);
    void attachTranscript(String guildId, String ticketId, String transcriptFileName);
    void markAutoCloseWarned(String guildId, String ticketId);
    void recordActivity(String guildId, String channelId);
}
