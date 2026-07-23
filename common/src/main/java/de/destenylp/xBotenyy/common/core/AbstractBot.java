package de.destenylp.xBotenyy.common.core;

import de.destenylp.xBotenyy.common.config.CommonConfig;
import de.destenylp.xBotenyy.common.persistence.BackupService;
import de.destenylp.xBotenyy.common.persistence.BackupSettings;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public abstract class AbstractBot {
    protected final Logger logger;
    protected final CommonConfig config;
    protected final ScheduledExecutorService scheduler;

    private final CountDownLatch shutdownLatch = new CountDownLatch(1);
    private volatile boolean shutdownHookRegistered = false;

    protected AbstractBot(Logger logger, CommonConfig config, String schedulerThreadName, int schedulerThreadCount) {
        this.logger = logger;
        this.config = config;
        this.scheduler = Executors.newScheduledThreadPool(schedulerThreadCount, threadFactory(schedulerThreadName));
    }

    private static ThreadFactory threadFactory(String name) {
        return runnable -> {
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            return thread;
        };
    }

    protected final void scheduleHeartbeat(long intervalMinutes) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                Runtime runtime = Runtime.getRuntime();
                long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
                long committedMb = runtime.totalMemory() / (1024 * 1024);
                long maxMb = runtime.maxMemory() / (1024 * 1024);
                logger.info("Heartbeat: heapUsedMb={} heapCommittedMb={} heapMaxMb={} threads={} {}",
                        usedMb, committedMb, maxMb, Thread.activeCount(), heartbeatSummary());
            } catch (Exception e) {
                logger.error("Fehler beim Schreiben des Heartbeats: ", e);
            }
        }, intervalMinutes, intervalMinutes, TimeUnit.MINUTES);
    }

    protected abstract String heartbeatSummary();

    protected final void scheduleDataRetention(long initialDelayMinutes, long intervalMinutes, Duration retention,
                                                List<PrunableResource> resources) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                StringBuilder summary = new StringBuilder();
                int total = 0;
                for (PrunableResource resource : resources) {
                    int pruned = resource.pruneOldEntries(retention);
                    total += pruned;
                    if (pruned > 0) {
                        if (summary.length() > 0) {
                            summary.append(", ");
                        }
                        summary.append(pruned).append(" ").append(resource.getName());
                    }
                }
                if (total > 0) {
                    logger.info("Datenbereinigung: {} aelter als {}h entfernt.", summary, retention.toHours());
                }
            } catch (Exception e) {
                logger.error("Fehler bei der Datenbereinigung: ", e);
            }
        }, initialDelayMinutes, intervalMinutes, TimeUnit.MINUTES);
    }

    protected final void scheduleBackup(BackupSettings settings, BackupService service) {
        if (!settings.enabled()) {
            logger.info("Automatische Datenbank-Backups sind deaktiviert (backup.enabled=false).");
            return;
        }
        service.start(settings.interval());
        logger.info("Backup-Zeitplan gestartet: alle {}h, {} Kopien werden aufbewahrt.",
                settings.interval().toHours(), settings.maxBackupsToKeep());
    }

    protected final synchronized void registerShutdownHook() {
        if (shutdownHookRegistered) {
            return;
        }
        shutdownHookRegistered = true;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown angefordert, beende Bot geordnet...");
            scheduler.shutdownNow();
            try {
                onShutdown();
            } catch (Exception e) {
                logger.error("Fehler beim Beenden des Bots: ", e);
            }
            logger.info("Bot wurde beendet.");
            shutdownLatch.countDown();
        }, "shutdown-hook"));
    }

    protected abstract void onShutdown();

    public final void awaitShutdown() throws InterruptedException {
        shutdownLatch.await();
    }
}
