package de.destenylp.xBotenyy.launcher;

import org.slf4j.Logger;

import java.time.Duration;

final class BotSupervisor {
    private final String botName;
    private final Runnable entryPoint;
    private final Logger logger;
    private final int maxRestartAttempts;
    private final Duration restartDelay;

    BotSupervisor(String botName, Runnable entryPoint, Logger logger, int maxRestartAttempts, Duration restartDelay) {
        this.botName = botName;
        this.entryPoint = entryPoint;
        this.logger = logger;
        this.maxRestartAttempts = maxRestartAttempts;
        this.restartDelay = restartDelay;
    }

    void runSupervised() {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                entryPoint.run();
                return;
            } catch (Throwable failure) {
                logger.error("{} ist abgestuerzt (Versuch {}/{}), der andere Bot laeuft unabhaengig davon weiter: ",
                        botName, attempt, maxRestartAttempts, failure);
                if (attempt >= maxRestartAttempts) {
                    logger.error("{} wird nach {} fehlgeschlagenen Versuchen nicht mehr automatisch neugestartet.",
                            botName, maxRestartAttempts);
                    return;
                }
                sleepQuietly(restartDelay);
                logger.info("Starte {} erneut (Versuch {}/{})...", botName, attempt + 1, maxRestartAttempts);
            }
        }
    }

    private static void sleepQuietly(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
