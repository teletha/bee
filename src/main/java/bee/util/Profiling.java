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

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import bee.UserInterface;

/**
 * A simple built-in profiler for measuring the execution time of code blocks.
 *
 * <p>
 * This profiler uses a static collection to store timing records globally. It supports
 * nested profiling scopes using a try-with-resources pattern. The time reported for
 * a scope represents the "self-time" – the time spent directly within that scope,
 * excluding time spent in nested (child) scopes.
 * </p>
 *
 * <p>
 * Usage Example:
 * </p>
 * <pre>{@code
 * // Start profiling the "Data Processing" task
 * try (Profiling ignored = Profiling.of("Data Processing")) {
 *     // Code for data processing...
 *
 *     // Start profiling a sub-task "Validation"
 *     try (Profiling ignored2 = Profiling.of("Validation")) {
 *         // Code for validation...
 *         Thread.sleep(50); // Simulate work
 *     } // "Validation" scope ends here
 *
 *     // More data processing code...
 *     Thread.sleep(100); // Simulate work
 * } // "Data Processing" scope ends here
 *
 * // Later, display the results
 * Profiling.show(ui); // Assuming 'ui' is a UserInterface instance
 * }</pre>
 *
 * <p>
 * In the example above, the time reported for "Data Processing" will be approximately 100ms,
 * as the 50ms spent in "Validation" is excluded from its parent's self-time. The time
 * for "Validation" will be approximately 50ms.
 * </p>
 *
 * <p>
 * Note: This profiler is designed for single-threaded usage scenarios or scenarios
 * where nesting occurs strictly within the same thread. The global state might lead
 * to incorrect nesting if {@code Profiling.of()} and {@code close()} calls are interleaved
 * across different threads without proper external synchronization. The collection of
 * results (`records`) is thread-safe.
 * </p>
 */
public class Profiling implements AutoCloseable {

    /** Stores all profiling records generated during execution. Thread-safe for additions. */
    private static final Queue<Profiling> records = new ConcurrentLinkedQueue();

    /** Tracks the currently active profiling scope. Used for handling nesting. */
    private static Profiling current = new Profiling(null, null);

    /** The descriptive name of this profiling scope. */
    private final String name;

    /** The parent profiling scope, used to restore state when this scope closes. */
    private Profiling previous;

    /**
     * The system time (nanoseconds) when this scope was initially started. (Seems unused for
     * elapsed calculation)
     */
    private long start;

    /**
     * The system time (nanoseconds) when this scope was finally stopped. (Seems unused for elapsed
     * calculation)
     */
    private long end;

    /** The system time (nanoseconds) when this scope was last started or resumed. */
    private long latest;

    /** The total accumulated time (nanoseconds) spent *directly* within this scope (self-time). */
    private long elapsed;

    /**
     * Private constructor to create a new profiling scope.
     *
     * @param name The name for this scope.
     * @param previous The scope that was active before this one started.
     */
    private Profiling(String name, Profiling previous) {
        this.name = name;
        this.previous = previous;
    }

    /**
     * Records the current system time as the start or resumption point for this scope.
     * If this is the first time {@code start()} is called for this instance, it sets the initial
     * {@code start} time.
     * It always updates the {@code latest} time marker.
     */
    private void start() {
        long now = System.nanoTime();

        if (start == 0) {
            start = now;
        }
        latest = now;
    }

    /**
     * Records the current system time as the end point for the current time slice
     * and adds the duration of this slice to the total {@code elapsed} time for this scope.
     * Also sets the absolute {@code end} time.
     */
    private void stop() {
        end = System.nanoTime();
        // Add time elapsed since the last 'start()' or 'resume()' call
        if (latest != 0) { // Ensure start() was called
            elapsed += end - latest;

            // Reset latest to prevent double counting if stop() is called multiple times
            latest = 0;
        }
    }

    /**
     * Stops the timer for the current scope and restores the previous scope as the active one.
     * This method is automatically called when using try-with-resources.
     * When the previous scope is restored, its timer is resumed.
     */
    @Override
    public void close() {
        stop();

        // Restore the previous scope as the current one
        // This ensures that the parent's timer resumes correctly after a nested scope finishes.
        current = this.previous;

        // If there was a parent scope, resume its timer
        if (current != null && current.name != null) {
            current.start(); // Resume parent's timer
        }
        this.previous = null; // Help GC
    }

    /**
     * Calculates the approximate JVM startup time until this method is called.
     * It creates a special "JVM startup" profiling record.
     * This relies on {@link ProcessHandle#info()} which might not be available or accurate on all
     * platforms/JVMs.
     * 
     * This method is somewhat special as it tries to measure time spent even before the
     * application's main logic (and potentially this profiler) is fully operational.
     */
    public static void calculateJVMStartup() {
        Instant end = Instant.now();
        Optional<Instant> start = ProcessHandle.current().info().startInstant();
        if (start.isPresent()) {
            Profiling profiling = new Profiling("JVM startup", null);
            profiling.elapsed = (end.toEpochMilli() - start.get().toEpochMilli()) * 1000 * 1000;
            records.add(profiling);
        }
    }

    /**
     * Starts a new profiling scope with the given name.
     *
     * <p>
     * This method should be used with try-with-resources:
     * </p>
     * <pre>{@code try (Profiling p = Profiling.of("My Task")) { ... } }</pre>
     *
     * <p>
     * When a new scope is started:
     * </p>
     * <ol>
     * <li>The currently active scope (if any) is paused.</li>
     * <li>A new {@link Profiling} instance is created and added to the global records.</li>
     * <li>The new instance becomes the {@code current} active scope.</li>
     * <li>The timer for the new scope is started.</li>
     * </ol>
     *
     * @param name The name of the scope to profile.
     * @return An {@link AutoCloseable} instance representing the new scope. Its {@link #close()}
     *         method
     *         must be called (typically via try-with-resources) to stop the timer and restore the
     *         previous scope.
     */
    public static Profiling of(String name) {
        // Pause the timer of the currently active scope before starting a new nested scope
        if (current != null) {
            current.stop();
        }

        records.add(current = new Profiling(name, current));
        current.start();
        return current;
    }

    /**
     * Aggregates the collected profiling data, formats it, and displays the top results
     * (longest durations first) using the provided {@link UserInterface}.
     * <p>
     * Only durations of 100ms or more are included. Up to the top 10 entries are shown.
     * The output format includes the duration (e.g., "1.234s") and the scope name.
     * </p>
     *
     * @param ui The {@link UserInterface} used for outputting the results.
     */
    public static void show(UserInterface ui) {
        Map<String, List<Profiling>> grouped = records.stream().collect(Collectors.groupingBy(v -> v.name));
        TreeMap<Long, String> output = new TreeMap(Comparator.reverseOrder());

        for (Entry<String, List<Profiling>> entry : grouped.entrySet()) {
            String name = entry.getKey();
            List<Profiling> values = entry.getValue();
            long sum = values.stream().mapToLong(v -> v.elapsed).sum() / 1000_000;
            if (50 <= sum) {
                output.put(sum, name);
            }
        }

        float total = output.keySet().stream().mapToLong(x -> x).sum();

        ui.title("Bee Profiler");
        ui.info("Since the profile option is enabled, show top 10 measurement items taking more than 50ms.");
        output.entrySet().stream().limit(10).forEach(entry -> {
            ui.info("%dms (%.1f%%)\t%s".formatted(entry.getKey(), (entry.getKey() / total) * 100, entry.getValue()));
        });
    }
}