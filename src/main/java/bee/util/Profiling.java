/*
 * Copyright (C) 2025 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.util;

import java.lang.management.ManagementFactory;
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
 * A thread-safe profiler for measuring the self-time execution of named code blocks,
 * designed to work with nested scopes and parallel execution environments, potentially
 * leveraging {@link InheritableThreadLocal} for parent scope tracking across threads
 * (especially effective with virtual threads).
 *
 * <p>
 * Use this profiler with a try-with-resources statement:
 * </p>
 * <pre>{@code try (Profiling ignored = Profiling.of("Your Scope Name")) { ... }}</pre>
 * <p>
 * Results are collected globally and can be displayed using {@link #show(UserInterface)}.
 * The reported time represents the "self-time" – time spent directly within a scope,
 * excluding time spent in nested child scopes started on the same logical thread
 * (as tracked by {@code InheritableThreadLocal}).
 * </p>
 */
public class Profiling implements AutoCloseable {

    /** Global, thread-safe storage for all completed profiling records. */
    private static final Queue<Profiling> records = new ConcurrentLinkedQueue<>();

    /**
     * Thread-local (inheritable) storage for the currently active profiling scope.
     * This attempts to track parent scopes even when tasks are delegated to child threads
     * (like virtual threads).
     */
    private static final InheritableThreadLocal<Profiling> current = new InheritableThreadLocal<>();

    /** The descriptive name of this profiling scope (e.g., "Task [compile:source]"). */
    private final String name;

    /** The parent scope as determined by the InheritableThreadLocal when this scope started. */
    private final Profiling parent;

    /** The system time (nanoseconds) when this scope was first started/resumed. */
    private long start;

    /** The system time (nanoseconds) when this scope was finally closed. */
    private long end;

    /** The system time (nanoseconds) when this scope was last resumed. */
    private long latestResume;

    /** The total accumulated self-time (nanoseconds) spent directly within this scope. */
    private long elapsed;

    /**
     * Private constructor. Initializes the scope name and parent link.
     * Timing starts upon the first call to {@link #resume()}.
     *
     * @param name The name of the scope (null allowed only for internal root).
     * @param parent The parent scope from the current thread context.
     */
    private Profiling(String name, Profiling parent) {
        this.name = name;
        this.parent = parent;
    }

    /**
     * Resumes or starts the timer for this scope. Called when the scope becomes active.
     */
    private void resume() {
        long now = System.nanoTime();

        // First time this scope instance is started
        if (start == 0) {
            start = now;
        }
        latestResume = now; // Mark the latest resume time
    }

    /**
     * Pauses the timer for this scope. Called when a nested scope starts or this scope closes.
     * Accumulates the elapsed time since the last resume.
     */
    private void pause() {
        end = System.nanoTime(); // Mark the potential end time
        if (latestResume != 0) { // Check if it was actually running
            elapsed += end - latestResume;
            latestResume = 0; // Mark as paused
        }
    }

    /**
     * Stops the timer, records the profiling data (if named), restores the parent scope
     * for the current thread, and resumes the parent's timer.
     * Automatically called by the try-with-resources statement.
     */
    @Override
    public void close() {
        pause(); // Finalize timing for this scope

        // Add the completed record to the global queue if it's a named scope
        if (this.name != null) {
            records.add(this);
        }

        // Restore the parent scope in the thread-local variable
        current.set(this.parent);

        // Resume the parent's timer if it exists and is a named scope
        if (this.parent != null && this.parent.name != null) {
            this.parent.resume();
        }
    }

    /**
     * Records the approximate JVM startup time as a special profiling entry.
     */
    public static void measureJVMStartup() {
        Instant endTime = Instant.now();
        Optional<Instant> startTimeOpt = ProcessHandle.current().info().startInstant();
        if (startTimeOpt.isPresent()) {
            Instant startTime = startTimeOpt.get();
            Profiling startupRecord = new Profiling("JVM startup", null);
            startupRecord.elapsed = (endTime.toEpochMilli() - startTime.toEpochMilli()) * 1_000_000L;
            startupRecord.start = startTime.toEpochMilli() * 1_000_000L;
            startupRecord.end = endTime.toEpochMilli() * 1_000_000L;
            records.add(startupRecord);
        }
    }

    /**
     * Starts a new profiling scope with the given name for the current thread.
     *
     * <p>
     * This method MUST be used within a try-with-resources statement:
     * </p>
     * <pre>{@code try (Profiling p = Profiling.of("Scope Name")) { ... }}</pre>
     *
     * @param name The descriptive name for this profiling scope (e.g., "Compile", "Network IO").
     *            Must not be null.
     * @return An {@link AutoCloseable} {@code Profiling} instance representing the new active scope
     *         for this thread.
     * @throws NullPointerException if the name is null.
     */
    public static Profiling of(String name) {
        Profiling parent = current.get();

        // Pause the parent scope's timer if it's a real scope
        if (parent != null && parent.name != null) {
            parent.pause();
        }

        // Create the new scope, linking it to the parent from this thread
        Profiling child = new Profiling(name, parent);

        // Set the new scope as current for this thread *before* resuming
        current.set(child);

        // Start the timer for the new scope *after* it's set as current
        child.resume();

        // Don't add to 'records' here; added when closed.
        return child;
    }

    /**
     * Aggregates profiling results from all threads, formats them, and displays a summary.
     * <p>
     * Shows the top 10 scopes ranked by total self-time (minimum 1ms displayed), sorted
     * descendingly.
     * Percentages indicate contribution relative to the total time of the *displayed* top scopes.
     * Includes JVM input arguments for context. Cleans up the current thread's profiler state.
     * </p>
     *
     * @param ui The {@link UserInterface} used for displaying the results.
     */
    public static void show(UserInterface ui) {
        // for (Profiling p : records) {
        // System.out.println(p.name + " start: " + p.start + " end: " + p.end + " elapsed: " +
        // p.elapsed);
        // }

        Map<String, List<Profiling>> grouped = records.stream().collect(Collectors.groupingBy(v -> v.name));
        TreeMap<Long, String> output = new TreeMap(Comparator.reverseOrder());

        for (Entry<String, List<Profiling>> entry : grouped.entrySet()) {
            String name = entry.getKey();
            List<Profiling> values = entry.getValue();
            long sum = values.stream().mapToLong(v -> v.elapsed).sum() / 1000_000;
            if (10 <= sum) {
                output.put(sum, name);
            }
        }

        float total = output.keySet().stream().mapToLong(x -> x).sum();
        List<String> results = output.entrySet()
                .stream()
                .limit(10)
                .map(entry -> "%dms (%.1f%%)  \t%s".formatted(entry.getKey(), (entry.getKey() / total) * 100, entry.getValue()))
                .toList();

        ui.title("Bee Profiler");
        ui.info("Used JVM options :", ManagementFactory.getRuntimeMXBean().getInputArguments());
        ui.info("Displays the top " + results.size() + " measured values that took more than 10 ms :", results);
    }
}