package de.destenylp.xBotenyy.common.core;

import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public abstract class AbstractHttpApiClient {
    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final Duration DEFAULT_BASE_RETRY_DELAY = Duration.ofSeconds(2);

    protected final Duration requestTimeout;
    protected final HttpClient httpClient;
    private final int maxAttempts;
    private final Duration baseRetryDelay;

    protected AbstractHttpApiClient(Duration requestTimeout) {
        this(requestTimeout, DEFAULT_MAX_ATTEMPTS, DEFAULT_BASE_RETRY_DELAY);
    }

    protected AbstractHttpApiClient(Duration requestTimeout, int maxAttempts, Duration baseRetryDelay) {
        this.requestTimeout = requestTimeout;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.baseRetryDelay = baseRetryDelay;
        HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(requestTimeout);
        customizeHttpClient(builder);
        this.httpClient = builder.build();
    }

    protected void customizeHttpClient(HttpClient.Builder builder) {
    }

    protected HttpRequest.Builder requestBuilder(URI uri) {
        return HttpRequest.newBuilder(uri).timeout(requestTimeout);
    }

    protected <T> HttpResponse<T> sendWithRetry(HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler,
                                                 Logger logger, String description) throws IOException, InterruptedException {
        IOException lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpResponse<T> response = httpClient.send(request, bodyHandler);
                if (!isRetryableStatus(response.statusCode()) || attempt == maxAttempts) {
                    return response;
                }
                logger.warn("{} lieferte Status {} (Versuch {}/{}), erneuter Versuch folgt.",
                        description, response.statusCode(), attempt, maxAttempts);
                sleepBeforeRetry(attempt);
            } catch (IOException e) {
                lastError = e;
                if (attempt == maxAttempts) {
                    throw e;
                }
                logger.warn("{} fehlgeschlagen (Versuch {}/{}): {}", description, attempt, maxAttempts, e.getMessage());
                sleepBeforeRetry(attempt);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Wiederholung von '" + description + "' wurde unterbrochen.", e);
            }
        }
        throw lastError != null ? lastError : new IOException("HTTP-Anfrage '" + description + "' fehlgeschlagen.");
    }

    private boolean isRetryableStatus(int statusCode) {
        return statusCode >= 500 && statusCode < 600;
    }

    private void sleepBeforeRetry(int attemptNumber) throws InterruptedException {
        long delayMillis = baseRetryDelay.toMillis() * (1L << (attemptNumber - 1));
        Thread.sleep(delayMillis);
    }
}
