package de.destenylp.xBotenyy.discordbot.util;

import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.RestAction;
import org.slf4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class RetryingRestAction {
    private static volatile int maxAttempts = 3;
    private static volatile long baseDelaySeconds = 2;

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(retryThreadFactory());

    private RetryingRestAction() {
    }

    public static void configure(int maxAttempts, long baseDelaySeconds) {
        RetryingRestAction.maxAttempts = Math.max(1, maxAttempts);
        RetryingRestAction.baseDelaySeconds = Math.max(1, baseDelaySeconds);
    }

    private static ThreadFactory retryThreadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable, "discord-retry-scheduler");
            thread.setDaemon(true);
            return thread;
        };
    }

    public static <T> void queueWithRetry(Supplier<RestAction<T>> actionSupplier, Consumer<T> onSuccess,
                                           Consumer<Throwable> onFailure, Logger logger, String description) {
        attempt(actionSupplier, onSuccess, onFailure, logger, description, 1);
    }

    private static <T> void attempt(Supplier<RestAction<T>> actionSupplier, Consumer<T> onSuccess,
                                     Consumer<Throwable> onFailure, Logger logger, String description, int attemptNumber) {
        actionSupplier.get().queue(onSuccess, failure -> {
            if (isRetryable(failure) && attemptNumber < maxAttempts) {
                long delay = baseDelaySeconds * (1L << (attemptNumber - 1));
                logger.warn("{} fehlgeschlagen (Versuch {}/{}), erneuter Versuch in {}s: {}",
                        description, attemptNumber, maxAttempts, delay, failure.getMessage());
                SCHEDULER.schedule(
                        () -> attempt(actionSupplier, onSuccess, onFailure, logger, description, attemptNumber + 1),
                        delay, TimeUnit.SECONDS);
            } else {
                onFailure.accept(failure);
            }
        });
    }

    private static boolean isRetryable(Throwable failure) {
        if (failure instanceof ErrorResponseException responseException) {
            ErrorResponse response = responseException.getErrorResponse();
            return response == ErrorResponse.SERVER_ERROR;
        }
        return false;
    }
}
