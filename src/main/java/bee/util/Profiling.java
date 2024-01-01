/*
 * Copyright (C) 2024 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.util;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import bee.UserInterface;
import kiss.I;

/**
 * Builtin simple profiler.
 */
public class Profiling implements AutoCloseable {

    /** All records. */
    private static final Queue<Profiling> records = new ConcurrentLinkedQueue();

    /** The current record. */
    private static Profiling current = new Profiling("", null);

    /** The name. */
    private final String name;

    private Profiling previous;

    /** The starting time. */
    private long start;

    /** The ending time. */
    private long end;

    /** The latest recorded time. */
    private long latest;

    /** The elapsed time of the specified phase. */
    private long elapsed;

    /**
     * 
     */
    private Profiling(String name, Profiling previous) {
        this.name = name;
        this.previous = previous;
    }

    /**
     * 
     */
    private void start() {
        long now = System.nanoTime();

        if (start == 0) {
            start = now;
        }
        latest = now;
    }

    /**
     * 
     */
    private void stop() {
        end = System.nanoTime();
        elapsed += end - latest;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        stop();
        if (current != null) {
            current = current.previous;
            if (current != null) current.start();
        }
    }

    /**
     * Start profiling.
     * 
     * @param name
     * @return
     */
    public static Profiling of(String name) {
        records.add(current = new Profiling(name, current));
        current.start();
        return current;
    }

    public static void show(UserInterface ui) {
        Map<String, List<Profiling>> grouped = records.stream().collect(Collectors.groupingBy(v -> v.name));
        TreeMap<Duration, String> output = new TreeMap(Comparator.reverseOrder());

        for (Entry<String, List<Profiling>> entry : grouped.entrySet()) {
            String name = entry.getKey();
            List<Profiling> values = entry.getValue();
            long total = values.stream().mapToLong(v -> v.elapsed).sum();
            Duration duration = Duration.ofNanos(total).truncatedTo(ChronoUnit.MILLIS);
            if (100 <= duration.toMillis()) {
                String time = duration.toString().substring(2).toLowerCase();
                output.put(duration, StringUtils.rightPad(time, 7) + "\t" + name);
            }
        }

        I.signal(output.values()).take(10).to(v -> {
            ui.info(v);
        });
    }
}