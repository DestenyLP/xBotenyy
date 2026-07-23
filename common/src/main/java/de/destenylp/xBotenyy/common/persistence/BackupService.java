package de.destenylp.xBotenyy.common.persistence;

import de.destenylp.xBotenyy.common.persistence.sql.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public final class BackupService implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackupService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneOffset.UTC);

    private final Database database;
    private final Path backupDirectory;
    private final String filePrefix;
    private final int maxBackupsToKeep;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "backup-service");
        thread.setDaemon(true);
        return thread;
    });

    public BackupService(Database database, Path backupDirectory, String filePrefix, int maxBackupsToKeep) {
        this.database = database;
        this.backupDirectory = backupDirectory;
        this.filePrefix = filePrefix;
        this.maxBackupsToKeep = Math.max(maxBackupsToKeep, 1);
    }

    public void start(Duration interval) {
        long intervalMinutes = Math.max(interval.toMinutes(), 1);
        scheduler.scheduleAtFixedRate(this::runBackupSafely, 0, intervalMinutes, TimeUnit.MINUTES);
    }

    private void runBackupSafely() {
        try {
            createBackup();
        } catch (Exception e) {
            LOGGER.error("Fehler beim Erstellen des Datenbank-Backups: ", e);
        }
    }

    public Path createBackup() {
        try {
            Files.createDirectories(backupDirectory);
        } catch (IOException e) {
            throw new UncheckedIOException("Konnte Backup-Verzeichnis " + backupDirectory + " nicht anlegen", e);
        }

        String fileName = filePrefix + "-" + TIMESTAMP_FORMAT.format(Instant.now()) + ".sqlite";
        Path target = backupDirectory.resolve(fileName);

        database.backupInto(target);
        LOGGER.info("Datenbank-Backup erstellt: {}", target);

        enforceRetention();
        return target;
    }

    private void enforceRetention() {
        List<Path> backups = listBackups();
        if (backups.size() <= maxBackupsToKeep) {
            return;
        }
        for (Path stale : backups.subList(maxBackupsToKeep, backups.size())) {
            try {
                Files.deleteIfExists(stale);
                LOGGER.info("Altes Backup entfernt: {}", stale);
            } catch (IOException e) {
                LOGGER.warn("Konnte altes Backup {} nicht entfernen: {}", stale, e.getMessage());
            }
        }
    }

    public List<Path> listBackups() {
        if (!Files.isDirectory(backupDirectory)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(backupDirectory)) {
            return files
                    .filter(path -> path.getFileName().toString().startsWith(filePrefix)
                            && path.getFileName().toString().endsWith(".sqlite"))
                    .sorted(Comparator.comparing(this::lastModifiedSafely).reversed())
                    .toList();
        } catch (IOException e) {
            LOGGER.warn("Konnte Backup-Verzeichnis {} nicht lesen: {}", backupDirectory, e.getMessage());
            return List.of();
        }
    }

    private Instant lastModifiedSafely(Path path) {
        try {
            return Files.getLastModifiedTime(path).toInstant();
        } catch (IOException e) {
            return Instant.EPOCH;
        }
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }
}
