package de.destenylp.xBotenyy.discordbot.tickets;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class TicketTranscriptService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TicketTranscriptService.class);
    private static volatile int maxMessages = 1000;
    private static volatile Path transcriptDir = Paths.get("data", "transcripts");
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private TicketTranscriptService() {
    }

    public static void configure(int maxMessages, Path transcriptDir) {
        TicketTranscriptService.maxMessages = Math.max(1, maxMessages);
        TicketTranscriptService.transcriptDir = transcriptDir;
    }

    public static CompletableFuture<TranscriptFile> generate(TextChannel channel, Ticket ticket) {
        return channel.getIterableHistory().takeAsync(maxMessages)
                .thenApply(messages -> writeTranscript(channel, ticket, messages))
                .exceptionally(failure -> {
                    LOGGER.error("Transcript für Ticket {} konnte nicht erstellt werden: {}", ticket.getId(), failure.getMessage());
                    return null;
                });
    }

    private static TranscriptFile writeTranscript(TextChannel channel, Ticket ticket, List<Message> messages) {
        List<Message> chronological = messages.stream()
                .sorted(Comparator.comparing(Message::getTimeCreated))
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("Transcript – Ticket #").append(ticket.getId()).append(" · ").append(ticket.getSubject()).append('\n');
        sb.append("Kanal: #").append(channel.getName()).append(" (").append(channel.getId()).append(")\n");
        sb.append("Erstellt von: ").append(ticket.getAuthorName()).append(" (").append(ticket.getAuthorId()).append(")\n");
        sb.append("Kategorie: ").append(ticket.getCategory().getLabel()).append('\n');
        sb.append("=".repeat(60)).append("\n\n");

        for (Message message : chronological) {
            String timestamp = TIMESTAMP.format(message.getTimeCreated().toInstant());
            String author = message.getAuthor().getName() + " (" + message.getAuthor().getId() + ")";
            sb.append('[').append(timestamp).append("] ").append(author).append(":\n");
            String content = message.getContentDisplay();
            sb.append(content.isBlank() ? "_[kein Textinhalt]_" : content).append('\n');
            message.getAttachments().forEach(attachment -> sb.append("  \uD83D\uDCCE Anhang: ").append(attachment.getUrl()).append('\n'));
            sb.append('\n');
        }

        String fileName = "ticket-" + ticket.getId() + "-" + Instant.now().toEpochMilli() + ".txt";
        Path targetPath = transcriptDir.resolve(fileName);
        try {
            Files.createDirectories(transcriptDir);
            Files.writeString(targetPath, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Transcript-Datei {} konnte nicht gespeichert werden: {}", targetPath, e.getMessage());
        }

        return new TranscriptFile(fileName, sb.toString());
    }

    public record TranscriptFile(String fileName, String content) {
        public FileUpload toFileUpload() {
            return FileUpload.fromData(content.getBytes(StandardCharsets.UTF_8), fileName);
        }
    }
}
