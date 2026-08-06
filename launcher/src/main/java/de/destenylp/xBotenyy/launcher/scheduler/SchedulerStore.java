package de.destenylp.xBotenyy.launcher.scheduler;
import org.slf4j.Logger;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
public final class SchedulerStore {
    private static final String ENV_STORE_FILE = "LAUNCHER_SCHEDULER_FILE";
    private static final String DEFAULT_STORE_FILE = "scheduler-tasks.txt";
    private final Path storeFile;
    private final Logger logger;
    public SchedulerStore(Logger logger) {
        this.logger = logger;
        String configured = System.getenv(ENV_STORE_FILE);
        this.storeFile = Path.of(configured == null || configured.isBlank() ? DEFAULT_STORE_FILE : configured);
    }
    public List<ScheduledTask> load() {
        List<ScheduledTask> tasks = new ArrayList<>();
        if (!Files.exists(storeFile)) {
            return tasks;
        }
        try {
            List<String> lines = Files.readAllLines(storeFile, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line.isBlank()) {
                    continue;
                }
                try {
                    tasks.add(ScheduledTask.fromStoreLine(line));
                } catch (Exception e) {
                    logger.warn("Invalid line in {} is being skipped: {}", storeFile, line);
                }
            }
        } catch (IOException e) {
            logger.error("Scheduler file {} could not be read: ", storeFile, e);
        }
        return tasks;
    }
    public synchronized void save(List<ScheduledTask> tasks) {
        List<String> lines = new ArrayList<>();
        for (ScheduledTask task : tasks) {
            lines.add(task.toStoreLine());
        }
        try {
            Files.write(storeFile, lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            logger.error("Scheduler file {} could not be written: ", storeFile, e);
        }
    }
}
