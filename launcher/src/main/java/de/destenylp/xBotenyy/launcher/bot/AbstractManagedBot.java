package de.destenylp.xBotenyy.launcher.bot;

import de.destenylp.xBotenyy.common.core.AbstractBot;
import de.destenylp.xBotenyy.launcher.LauncherSettings;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractManagedBot<T extends AbstractBot> implements ManagedBot {

    private final BotId id;
    private final String displayName;
    private final Logger logger;
    private final LauncherSettings settings;

    private final Object lifecycleLock = new Object();
    private final AtomicReference<T> currentInstance = new AtomicReference<>();
    private final AtomicInteger restartCount = new AtomicInteger(0);

    private volatile BotStatus status = BotStatus.STOPPED;
    private volatile boolean manualStopRequested = false;
    private volatile long lastStartedAtMillis = -1;

    protected AbstractManagedBot(BotId id, String displayName, Logger logger, LauncherSettings settings) {
        this.id = id;
        this.displayName = displayName;
        this.logger = logger;
        this.settings = settings;
    }

    private static void sleepQuietly(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected abstract T createInstance();

    protected abstract void performStart(T instance) throws Exception;

    @Override
    public final BotId getId() {
        return id;
    }

    @Override
    public final String getDisplayName() {
        return displayName;
    }

    @Override
    public final BotStatus getStatus() {
        return status;
    }

    @Override
    public final int getRestartCount() {
        return restartCount.get();
    }

    @Override
    public final long getLastStartedAtMillis() {
        return lastStartedAtMillis;
    }

    @Override
    public final void start() {
        synchronized (lifecycleLock) {
            if (status == BotStatus.RUNNING || status == BotStatus.STARTING) {
                logger.warn("{} is already running (status={}), start command is ignored.", displayName, status);
                return;
            }
            manualStopRequested = false;
            restartCount.set(0);
            status = BotStatus.STARTING;
            Thread supervisorThread = new Thread(this::runSupervised, id.primaryName() + "-supervisor");
            supervisorThread.setUncaughtExceptionHandler((thread, error) -> {
                logger.error("{} was terminated by a fatal, unhandled error in the supervisor thread: ",
                        displayName, error);
                status = BotStatus.FAILED;
            });
            supervisorThread.start();
        }
    }

    @Override
    public final boolean stop(long timeoutSeconds) {
        synchronized (lifecycleLock) {
            if (status == BotStatus.STOPPED) {
                logger.info("{} is already stopped.", displayName);
                return true;
            }
            manualStopRequested = true;
            status = BotStatus.STOPPING;
        }

        T instance = currentInstance.get();
        if (instance == null) {
            status = BotStatus.STOPPED;
            return true;
        }

        Thread stopWorker = new Thread(instance::shutdown, id.primaryName() + "-stop");
        stopWorker.setDaemon(true);
        stopWorker.start();
        try {
            stopWorker.join(Duration.ofSeconds(Math.max(timeoutSeconds, 1)).toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean stoppedInTime = !stopWorker.isAlive();
        if (stoppedInTime) {
            status = BotStatus.STOPPED;
            logger.info("{} was stopped successfully.", displayName);
        } else {
            logger.warn("{} did not respond to the stop command within {}s - the shutdown continues in the "
                            + "background, the status will be updated once it completes.",
                    displayName, timeoutSeconds);
        }
        return stoppedInTime;
    }

    private void runSupervised() {
        int attempt = 0;
        while (true) {
            attempt++;
            T instance;
            try {
                instance = createInstance();
            } catch (Throwable configFailure) {
                logger.error("{} could not be started, configuration is invalid: {}",
                        displayName, configFailure.getMessage());
                status = BotStatus.FAILED;
                return;
            }
            currentInstance.set(instance);

            try {
                performStart(instance);
                lastStartedAtMillis = System.currentTimeMillis();
                status = BotStatus.RUNNING;
                logger.info("{} was started successfully (attempt {}).", displayName, attempt);

                instance.awaitShutdown();

                status = BotStatus.STOPPED;
                return;
            } catch (Throwable failure) {
                int maxAttempts = settings.getMaxRestartAttempts();
                logger.error("{} has crashed (attempt {}/{}), the other bot keeps running independently: ",
                        displayName, attempt, maxAttempts, failure);

                if (manualStopRequested) {
                    status = BotStatus.STOPPED;
                    return;
                }
                if (attempt >= maxAttempts) {
                    logger.error("{} will no longer be restarted automatically after {} failed attempts. "
                                    + "Use 'start {}' to restart it manually.",
                            displayName, maxAttempts, id.primaryName());
                    status = BotStatus.FAILED;
                    return;
                }

                status = BotStatus.CRASHED;
                restartCount.incrementAndGet();
                Duration delay = Duration.ofSeconds(settings.getRestartDelaySeconds());
                sleepQuietly(delay);

                if (manualStopRequested) {
                    status = BotStatus.STOPPED;
                    return;
                }
                logger.info("Restarting {} (attempt {}/{}, wait time was {}s)...",
                        displayName, attempt + 1, maxAttempts, delay.toSeconds());
            }
        }
    }
}
