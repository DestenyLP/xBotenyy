package de.destenylp.xBotenyy.discordbot.tickets;

import de.destenylp.xBotenyy.discordbot.observability.BotMetrics;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class TicketCloseCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger(TicketCloseCoordinator.class);

    private final TicketService service;
    private final long channelDeleteDelaySeconds;

    public TicketCloseCoordinator(TicketService service) {
        this(service, 20);
    }

    public TicketCloseCoordinator(TicketService service, long channelDeleteDelaySeconds) {
        this.service = service;
        this.channelDeleteDelaySeconds = Math.max(0, channelDeleteDelaySeconds);
    }

    public void finalizeClose(TextChannel channel, Ticket ticket) {
        JDA jda = channel.getJDA();

        channel.sendMessageEmbeds(TicketEmbedFactory.buildTicketEmbed(ticket))
                .setComponents(TicketEmbedFactory.buildTicketComponents(ticket))
                .queue(success -> { }, failure -> LOGGER.warn("Abschluss-Embed konnte nicht gepostet werden: {}", failure.getMessage()));

        TicketTranscriptService.generate(channel, ticket).thenAccept(transcript -> {
            if (transcript != null) {
                service.attachTranscript(ticket.getGuildId(), ticket.getId(), transcript.fileName());
            }
            postToLogChannel(jda, ticket, transcript);
            requestRating(jda, ticket);
            scheduleChannelDeletion(channel, ticket);
        });

        LOGGER.info("Ticket {} in guild {} closed, transcript generation started", ticket.getId(), ticket.getGuildId());
        BotMetrics.incrementTicketsClosed();
    }

    private void postToLogChannel(JDA jda, Ticket ticket, TicketTranscriptService.TranscriptFile transcript) {
        TicketSettings settings = service.getSettings(ticket.getGuildId()).orElse(null);
        if (settings == null) {
            LOGGER.warn("Keine Ticket-Einstellungen für Guild {} gefunden, Ticket {} wird nicht archiviert", ticket.getGuildId(), ticket.getId());
            return;
        }

        String logChannelId = settings.getLogChannelId();
        String transcriptChannelId = settings.getTranscriptChannelId();

        if (logChannelId == null && transcriptChannelId == null) {
            LOGGER.warn("Für Ticket {} in Guild {} ist weder ein Log- noch ein Transcript-Kanal konfiguriert. Nutze /ticket settings logchannel:#kanal, damit geschlossene Tickets protokolliert werden.",
                    ticket.getId(), ticket.getGuildId());
            return;
        }

        if (logChannelId != null) {
            TextChannel logChannel = jda.getChannelById(TextChannel.class, logChannelId);
            if (logChannel == null) {
                LOGGER.warn("Konfigurierter Log-Kanal {} für Ticket {} existiert nicht mehr", logChannelId, ticket.getId());
            } else {
                var action = logChannel.sendMessageEmbeds(TicketEmbedFactory.buildLogEmbed(ticket));
                boolean attachTranscriptHere = transcript != null && (transcriptChannelId == null || transcriptChannelId.equals(logChannelId));
                if (attachTranscriptHere) {
                    action = action.setFiles(transcript.toFileUpload());
                }
                action.queue(
                        message -> service.attachLogMessage(ticket.getGuildId(), ticket.getId(), logChannelId, message.getId()),
                        failure -> LOGGER.error("Log-Eintrag für Ticket {} konnte nicht in Kanal {} gepostet werden: {}",
                                ticket.getId(), logChannelId, failure.getMessage()));
            }
        }

        if (transcript != null && transcriptChannelId != null && !transcriptChannelId.equals(logChannelId)) {
            TextChannel transcriptChannel = jda.getChannelById(TextChannel.class, transcriptChannelId);
            if (transcriptChannel == null) {
                LOGGER.warn("Konfigurierter Transcript-Kanal {} für Ticket {} existiert nicht mehr", transcriptChannelId, ticket.getId());
            } else {
                transcriptChannel.sendMessage("Transcript für Ticket #" + ticket.getId() + " · " + ticket.getSubject())
                        .setFiles(transcript.toFileUpload())
                        .queue(success -> { }, failure -> LOGGER.error("Transcript für Ticket {} konnte nicht in Kanal {} gepostet werden: {}",
                                ticket.getId(), transcriptChannelId, failure.getMessage()));
            }
        }
    }

    public void refreshLogMessage(JDA jda, Ticket ticket) {
        if (ticket.getLogChannelId() == null || ticket.getLogMessageId() == null) {
            return;
        }
        TextChannel logChannel = jda.getChannelById(TextChannel.class, ticket.getLogChannelId());
        if (logChannel == null) {
            LOGGER.warn("Log-Kanal {} für Ticket {} existiert nicht mehr, Bewertung konnte nicht nachgetragen werden", ticket.getLogChannelId(), ticket.getId());
            return;
        }
        logChannel.editMessageEmbedsById(ticket.getLogMessageId(), TicketEmbedFactory.buildLogEmbed(ticket))
                .queue(success -> { }, failure -> LOGGER.warn("Log-Eintrag {} für Ticket {} konnte nicht mit Bewertung aktualisiert werden: {}",
                        ticket.getLogMessageId(), ticket.getId(), failure.getMessage()));
    }

    private void requestRating(JDA jda, Ticket ticket) {
        jda.retrieveUserById(ticket.getAuthorId()).queue(
                user -> user.openPrivateChannel().queue(
                        dm -> dm.sendMessageEmbeds(TicketEmbedFactory.buildRatingRequestEmbed(ticket))
                                .setComponents(TicketEmbedFactory.buildRatingComponents(ticket))
                                .queue(success -> { }, failure -> LOGGER.debug("Konnte Bewertungsanfrage nicht an {} senden: {}",
                                        ticket.getAuthorId(), failure.getMessage())),
                        failure -> LOGGER.debug("Konnte keinen DM-Kanal mit {} öffnen: {}", ticket.getAuthorId(), failure.getMessage())),
                failure -> LOGGER.debug("Konnte Nutzer {} nicht auflösen: {}", ticket.getAuthorId(), failure.getMessage()));
    }

    private void scheduleChannelDeletion(TextChannel channel, Ticket ticket) {
        channel.sendMessage("\uD83D\uDD12 Dieses Ticket wird in " + channelDeleteDelaySeconds + " Sekunden archiviert...")
                .queue(success -> { }, failure -> { });
        channel.delete()
                .reason("Ticket #" + ticket.getId() + " geschlossen")
                .queueAfter(channelDeleteDelaySeconds, TimeUnit.SECONDS, success -> { },
                        failure -> LOGGER.warn("Ticket-Kanal {} konnte nicht gelöscht werden: {}", channel.getId(), failure.getMessage()));
    }
}
