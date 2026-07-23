package de.destenylp.xBotenyy.common.observability;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public final class Metrics {
    private static final Map<String, LongAdder> COUNTERS = new ConcurrentHashMap<>();

    private Metrics() {
    }

    public static void increment(String name) {
        increment(name, 1);
    }

    public static void increment(String name, long amount) {
        COUNTERS.computeIfAbsent(name, ignored -> new LongAdder()).add(amount);
    }

    public static long get(String name) {
        LongAdder adder = COUNTERS.get(name);
        return adder == null ? 0L : adder.sum();
    }

    public static Map<String, Long> snapshot() {
        Map<String, Long> result = new TreeMap<>();
        COUNTERS.forEach((name, adder) -> result.put(name, adder.sum()));
        return result;
    }

    public static void reset(String name) {
        COUNTERS.remove(name);
    }
}
