package de.destenylp.xBotenyy.launcher.scheduler;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class ScheduledTask {
    private static final DateTimeFormatter DAILY_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private final String id;
    private final String target;
    private final ScheduledAction action;
    private final ScheduleType type;
    private final long intervalMillis;
    private final LocalTime dailyTime;
    private final long timeoutSeconds;
    private final AtomicBoolean enabled;
    private final AtomicLong nextRunMillis;
    private final AtomicLong lastRunMillis;

    private ScheduledTask(String id, String target, ScheduledAction action, ScheduleType type,
                          long intervalMillis, LocalTime dailyTime, long timeoutSeconds, boolean enabled,
                          long nextRunMillis, long lastRunMillis) {
        this.id = id;
        this.target = target;
        this.action = action;
        this.type = type;
        this.intervalMillis = intervalMillis;
        this.dailyTime = dailyTime;
        this.timeoutSeconds = timeoutSeconds;
        this.enabled = new AtomicBoolean(enabled);
        this.nextRunMillis = new AtomicLong(nextRunMillis);
        this.lastRunMillis = new AtomicLong(lastRunMillis);
    }

    public static ScheduledTask interval(String id, String target, ScheduledAction action, Duration interval,
                                         long timeoutSeconds) {
        ScheduledTask task = new ScheduledTask(id, target, action, ScheduleType.INTERVAL, interval.toMillis(),
                null, timeoutSeconds, true, -1, -1);
        task.nextRunMillis.set(System.currentTimeMillis() + interval.toMillis());
        return task;
    }

    public static ScheduledTask daily(String id, String target, ScheduledAction action, LocalTime dailyTime,
                                      long timeoutSeconds) {
        ScheduledTask task = new ScheduledTask(id, target, action, ScheduleType.DAILY, -1, dailyTime,
                timeoutSeconds, true, -1, -1);
        task.nextRunMillis.set(computeNextDailyRun(dailyTime, System.currentTimeMillis()));
        return task;
    }

    static ScheduledTask restore(String id, String target, ScheduledAction action, ScheduleType type,
                                 long intervalMillis, LocalTime dailyTime, long timeoutSeconds, boolean enabled,
                                 long lastRunMillis) {
        ScheduledTask task = new ScheduledTask(id, target, action, type, intervalMillis, dailyTime, timeoutSeconds,
                enabled, -1, lastRunMillis);
        task.nextRunMillis.set(task.computeInitialNextRun());
        return task;
    }

    private static long computeNextDailyRun(LocalTime time, long fromMillis) {
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime from = Instant.ofEpochMilli(fromMillis).atZone(zone);
        ZonedDateTime candidate = from.toLocalDate().atTime(time).atZone(zone);
        if (!candidate.isAfter(from)) {
            candidate = candidate.plusDays(1);
        }
        return candidate.toInstant().toEpochMilli();
    }

    public static ScheduledTask fromStoreLine(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length != 8) {
            throw new IllegalArgumentException("Ungueltige Scheduler-Zeile: " + line);
        }
        String id = parts[0];
        String target = parts[1];
        ScheduledAction action = ScheduledAction.valueOf(parts[2]);
        ScheduleType type = ScheduleType.valueOf(parts[3]);
        long timeoutSeconds = Long.parseLong(parts[5]);
        boolean enabled = Boolean.parseBoolean(parts[6]);
        long lastRunMillis = Long.parseLong(parts[7]);
        if (type == ScheduleType.DAILY) {
            LocalTime dailyTime = LocalTime.parse(parts[4], DAILY_TIME_FORMAT);
            return restore(id, target, action, type, -1, dailyTime, timeoutSeconds, enabled, lastRunMillis);
        }
        long intervalMillis = Long.parseLong(parts[4]);
        return restore(id, target, action, type, intervalMillis, null, timeoutSeconds, enabled, lastRunMillis);
    }

    private long computeInitialNextRun() {
        long now = System.currentTimeMillis();
        if (type == ScheduleType.DAILY) {
            return computeNextDailyRun(dailyTime, now);
        }
        if (lastRunMillis.get() < 0) {
            return now + intervalMillis;
        }
        long next = lastRunMillis.get() + intervalMillis;
        return next > now ? next : now;
    }

    public void markExecuted(long executedAtMillis) {
        lastRunMillis.set(executedAtMillis);
        if (type == ScheduleType.DAILY) {
            nextRunMillis.set(computeNextDailyRun(dailyTime, executedAtMillis));
        } else {
            nextRunMillis.set(executedAtMillis + intervalMillis);
        }
    }

    public String getId() {
        return id;
    }

    public String getTarget() {
        return target;
    }

    public ScheduledAction getAction() {
        return action;
    }

    public ScheduleType getType() {
        return type;
    }

    public long getIntervalMillis() {
        return intervalMillis;
    }

    public LocalTime getDailyTime() {
        return dailyTime;
    }

    public long getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public void setEnabled(boolean value) {
        enabled.set(value);
        if (value) {
            nextRunMillis.set(computeInitialNextRun());
        }
    }

    public long getNextRunMillis() {
        return nextRunMillis.get();
    }

    public long getLastRunMillis() {
        return lastRunMillis.get();
    }

    public boolean isDue(long nowMillis) {
        return enabled.get() && nowMillis >= nextRunMillis.get();
    }

    public String describeSchedule() {
        if (type == ScheduleType.DAILY) {
            return "taeglich um " + DAILY_TIME_FORMAT.format(dailyTime);
        }
        return "alle " + DurationParser.format(Duration.ofMillis(intervalMillis));
    }

    public String toStoreLine() {
        String valuePart = type == ScheduleType.DAILY
                ? DAILY_TIME_FORMAT.format(dailyTime)
                : String.valueOf(intervalMillis);
        return String.join("|", id, target, action.name(), type.name(), valuePart,
                String.valueOf(timeoutSeconds), String.valueOf(enabled.get()), String.valueOf(lastRunMillis.get()));
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "[%s] %s %s (%s)%s", id, action, target, describeSchedule(),
                enabled.get() ? "" : " (deaktiviert)");
    }
}

