package de.destenylp.xBotenyy.launcher.scheduler;
import de.destenylp.xBotenyy.launcher.bot.BotRegistry;
import de.destenylp.xBotenyy.launcher.bot.ManagedBot;
import org.slf4j.Logger;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
public final class TaskScheduler {
    private static final List<String> ALL_TOKENS = List.of("all", "both", "*");
    private static final long DEFAULT_TICK_SECONDS = 10;
    private static final long DEFAULT_TIMEOUT_SECONDS = 30;
    private final BotRegistry registry;
    private final SchedulerStore store;
    private final Logger logger;
    private final List<ScheduledTask> tasks = new CopyOnWriteArrayList<>();
    private final AtomicInteger idCounter = new AtomicInteger(0);
    private ScheduledExecutorService executor;
    private ExecutorService actionExecutor;
    private volatile ScheduledFuture<?> tickFuture;
    public TaskScheduler(BotRegistry registry, SchedulerStore store, Logger logger) {
        this.registry = registry;
        this.store = store;
        this.logger = logger;
    }
    public void start() {
        for (ScheduledTask task : store.load()) {
            tasks.add(task);
            trackId(task.getId());
        }
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "task-scheduler-tick");
            thread.setDaemon(true);
            return thread;
        });
        actionExecutor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "task-scheduler-action");
            thread.setDaemon(true);
            return thread;
        });
        tickFuture = executor.scheduleAtFixedRate(this::tick, DEFAULT_TICK_SECONDS, DEFAULT_TICK_SECONDS,
                TimeUnit.SECONDS);
        logger.info("TaskScheduler started ({} saved tasks loaded).", tasks.size());
    }
    public void stop() {
        if (tickFuture != null) {
            tickFuture.cancel(false);
        }
        if (executor != null) {
            executor.shutdownNow();
        }
        if (actionExecutor != null) {
            actionExecutor.shutdown();
        }
        logger.info("TaskScheduler stopped.");
    }
    private void trackId(String id) {
        if (id != null && id.startsWith("s")) {
            try {
                int numeric = Integer.parseInt(id.substring(1));
                idCounter.updateAndGet(current -> Math.max(current, numeric));
            } catch (NumberFormatException ignored) {
            }
        }
    }
    private String nextId() {
        return "s" + idCounter.incrementAndGet();
    }
    public ScheduledTask addInterval(String target, ScheduledAction action, Duration interval, long timeoutSeconds) {
        validateTarget(target);
        ScheduledTask task = ScheduledTask.interval(nextId(), normalizeTarget(target), action, interval,
                timeoutSeconds);
        tasks.add(task);
        persist();
        return task;
    }
    public ScheduledTask addDaily(String target, ScheduledAction action, LocalTime dailyTime, long timeoutSeconds) {
        validateTarget(target);
        ScheduledTask task = ScheduledTask.daily(nextId(), normalizeTarget(target), action, dailyTime,
                timeoutSeconds);
        tasks.add(task);
        persist();
        return task;
    }
    public boolean remove(String id) {
        boolean removed = tasks.removeIf(task -> task.getId().equalsIgnoreCase(id));
        if (removed) {
            persist();
        }
        return removed;
    }
    public Optional<ScheduledTask> find(String id) {
        return tasks.stream().filter(task -> task.getId().equalsIgnoreCase(id)).findFirst();
    }
    public boolean setEnabled(String id, boolean enabled) {
        Optional<ScheduledTask> task = find(id);
        task.ifPresent(t -> {
            t.setEnabled(enabled);
            persist();
        });
        return task.isPresent();
    }
    public List<ScheduledTask> all() {
        return List.copyOf(tasks);
    }
    private void validateTarget(String target) {
        String normalized = target.toLowerCase(Locale.ROOT).trim();
        if (ALL_TOKENS.contains(normalized)) {
            return;
        }
        if (registry.find(normalized).isEmpty()) {
            throw new IllegalArgumentException("Unbekannter Bot '" + target + "'. Gueltige Werte: discord, twitch, all.");
        }
    }
    private String normalizeTarget(String target) {
        String normalized = target.toLowerCase(Locale.ROOT).trim();
        return ALL_TOKENS.contains(normalized) ? "all" : normalized;
    }
    private List<ManagedBot> resolveTargets(String target) {
        if (ALL_TOKENS.contains(target)) {
            return List.copyOf(registry.all());
        }
        return registry.find(target).map(List::of).orElse(List.of());
    }
    private void tick() {
        long now = System.currentTimeMillis();
        for (ScheduledTask task : tasks) {
            if (task.isDue(now)) {
                task.markExecuted(now);
                persist();
                actionExecutor.execute(() -> runTask(task));
            }
        }
    }
    private void runTask(ScheduledTask task) {
        List<ManagedBot> targets = resolveTargets(task.getTarget());
        if (targets.isEmpty()) {
            logger.warn("Scheduled task {} found no matching bots (target={}).", task.getId(),
                    task.getTarget());
            return;
        }
        long timeoutSeconds = task.getTimeoutSeconds() > 0 ? task.getTimeoutSeconds() : DEFAULT_TIMEOUT_SECONDS;
        for (ManagedBot bot : targets) {
            logger.info("Executing scheduled task {}: {} {}", task.getId(), task.getAction(),
                    bot.getDisplayName());
            switch (task.getAction()) {
                case RESTART -> {
                    bot.stop(timeoutSeconds);
                    bot.start();
                }
                case STOP -> bot.stop(timeoutSeconds);
                case START -> bot.start();
            }
        }
    }
    private void persist() {
        store.save(tasks);
    }
}
