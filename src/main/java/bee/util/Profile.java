/*
 * Copyright (C) 2022 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.util;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import bee.UserInterface;

/**
 * Builtin simple profiler.
 */
public class Profile {

    /** All records. */
    private static final Queue<Profile> records = new ConcurrentLinkedQueue();

    /** The actual measurement device. */
    public final MeasurementDevice start;

    /** The name. */
    private final String name;

    /** The starting time. */
    private final long from;

    /** The ending time. */
    private long to;

    /**
     * 
     */
    private Profile(String name) {
        this.name = name;
        this.start = () -> {
            to = System.nanoTime();
            records.add(this);
        };
        this.from = System.nanoTime();
    }

    /**
     * Start profiling.
     * 
     * @param name
     * @return
     */
    public static Profile of(String name) {
        return new Profile(name);
    }

    public static void show(UserInterface ui) {
        Map<String, List<Profile>> grouped = records.stream().collect(Collectors.groupingBy(v -> v.name));
        for (Entry<String, List<Profile>> entry : grouped.entrySet()) {
            String name = entry.getKey();
            List<Profile> values = entry.getValue();
            long total = values.stream().mapToLong(v -> v.to - v.from).sum();

            ui.info(name, " \tcall: ", values.size(), " \ttotal: ", total, "ns");
        }
    }

    /**
     * 
     */
    public interface MeasurementDevice extends AutoCloseable {

        /**
         * {@inheritDoc}
         */
        @Override
        void close();
    }
}
