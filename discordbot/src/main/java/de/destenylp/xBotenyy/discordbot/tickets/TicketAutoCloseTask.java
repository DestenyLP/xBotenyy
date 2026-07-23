package de.destenylp.xBotenyy.discordbot.tickets;

import de.destenylp.xBotenyy.discordbot.observability.BotMetrics;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public final class TicketAutoCloseTask implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(TicketAutoCloseTask.class);

    private final JDA jda;
    private final TicketService service;
    private final TicketCloseCoordinator closeCoordinator;

    public TicketAutoCloseTask(JDA jda, TicketService service, TicketCloseCoordinator closeCoordinator) {
        this.jda = jda;
        this.service = service;
        this.closeCoordinator = closeCoordinator;
    }

    @Override
    public void run() {
        try {
            sendWarnings();
            closeInactiveTickets();
        } catch (Exception e) {
            LOGGER.error("Fehler beim automatischen Schließen inaktiver Tickets: ", e);
        }
    }

    private void sendWarnings() {
        Map<String, List<Ticket>> due = service.findTicketsDueForAutoCloseWarning();
        due.forEach((guildId, tickets) -> tickets.forEach(ticket -> {
            TextChannel channel = resolveChannel(ticket);
            if (channel == null) {
                return;
            }
            channel.sendMessage("\u23F0 Dieses Ticket war längere Zeit inaktiv und wird automatisch geschlossen, " +
                            "falls keine weitere Aktivität stattfindet.")
                    .queue(success -> { }, failure -> { });
            service.markAutoCloseWarned(guildId, ticket.getId());
        }));
    }

    private void closeInactiveTickets() {
        Map<String, List<Ticket>> due = service.findTicketsDueForAutoClose();
        due.forEach((guildId, tickets) -> tickets.forEach(ticket -> {
            TextChannel channel = resolveChannel(ticket);
            if (channel == null) {
                return;
            }
            boolean success = service.close(guildId, ticket.getId(), null, null, "Automatisch geschlossen wegen Inaktivität");
            if (!success) {
                return;
            }
            Ticket closedTicket = service.getTicket(guildId, ticket.getId()).orElse(ticket);
            closeCoordinator.finalizeClose(channel, closedTicket);
            BotMetrics.incrementTicketsAutoClosed();
            LOGGER.info("Ticket {} in guild {} automatisch wegen Inaktivität geschlossen", ticket.getId(), guildId);
        }));
    }

    private TextChannel resolveChannel(Ticket ticket) {
        if (ticket.getChannelId() == null) {
            return null;
        }
        TextChannel channel = jda.getChannelById(TextChannel.class, ticket.getChannelId());
        if (channel == null) {
            LOGGER.warn("Ticket-Kanal {} für Ticket {} existiert nicht mehr", ticket.getChannelId(), ticket.getId());
        }
        return channel;
    }
}
