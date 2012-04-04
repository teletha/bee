/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.util;

import static java.util.concurrent.TimeUnit.*;

import java.util.concurrent.TimeUnit;

/**
 * An object that measures elapsed time in nanoseconds. Note that measurement overhead is typically
 * on the order of a microsecond (1000 ns) or more.
 * <p>
 * This class is not thread-safe.
 * <p>
 * Basic usage:
 * 
 * <pre>
 *   Stopwatch stopwatch = new Stopwatch().{@link #start()};
 *
 *   long millis = stopwatch.{@link #elapsedMillis()};
 *   long nanos  = stopwatch.{@link #elapsedTime}(TimeUnit.NANOSECONDS);
 *      // Measurement accuracy is really only to millis, but if you want ...
 *
 *   String formatted = stopwatch.{@link #toString()};  // e.g. "1.234 ms" or "23.45 s"
 *
 *   stopwatch.{@link #stop()};
 *   stopwatch.{@link #reset()}; // Resets the elapsed time to zero, stops the stopwatch.
 * </pre>
 * <p>
 * Note that it is an error to start or stop a Stopwatch that is already started or stopped
 * respectively.
 * <p>
 * When testing code that uses this class, use the {@linkplain #Stopwatch(Ticker) alternate
 * constructor} to supply a fake or mock ticker, such as
 * {@link com.google.common.testing.FakeTicker}. This allows you to simulate any valid behavior of
 * the stopwatch.
 * 
 * @author kevinb@google.com (Kevin Bourrillion)
 */
public final class Stopwatch {

    private boolean isRunning;

    private long elapsedNanos;

    private long startTick;

    /**
     * Returns {@code true} if {@link #start()} has been called on this stopwatch, and
     * {@link #stop()} has not been called since the last call to {@code start()}.
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Starts the stopwatch.
     * 
     * @throws IllegalStateException if the stopwatch is already running.
     */
    public Stopwatch start() {
        if (!isRunning) {
            isRunning = true;
            startTick = System.nanoTime();
        }
        return this;
    }

    /**
     * Stops the stopwatch. Future reads will return the fixed duration that had elapsed up to this
     * point.
     * 
     * @throws IllegalStateException if the stopwatch is already stopped.
     */
    public Stopwatch stop() {
        if (isRunning) {
            long tick = System.nanoTime();

            isRunning = false;
            elapsedNanos += tick - startTick;
        }

        return this;
    }

    /**
     * Sets the elapsed time for this stopwatch to zero, and places it in a stopped state.
     */
    public Stopwatch reset() {
        elapsedNanos = 0;
        isRunning = false;
        return this;
    }

    private long elapsedNanos() {
        return isRunning ? System.nanoTime() - startTick + elapsedNanos : elapsedNanos;
    }

    /**
     * Returns the current elapsed time shown on this stopwatch, expressed in the desired time unit,
     * with any fraction rounded down.
     * <p>
     * Note that the overhead of measurement can be more than a microsecond, so it is generally not
     * useful to specify {@link TimeUnit#NANOSECONDS} precision here.
     */
    public long elapsedTime(TimeUnit desiredUnit) {
        return desiredUnit.convert(elapsedNanos(), NANOSECONDS);
    }

    /**
     * Returns the current elapsed time shown on this stopwatch, expressed in milliseconds, with any
     * fraction rounded down. This is identical to {@code elapsedTime(TimeUnit.MILLISECONDS}.
     */
    public long elapsedMillis() {
        return elapsedTime(MILLISECONDS);
    }

    /**
     * Returns a string representation of the current elapsed time; equivalent to
     * {@code toString(4)} (four significant figures).
     */
    @Override
    public String toString() {
        return toString(4);
    }

    /**
     * Returns a string representation of the current elapsed time, choosing an appropriate unit and
     * using the specified number of significant figures. For example, at the instant when
     * {@code elapsedTime(NANOSECONDS)} would return {1234567}, {@code toString(4)} returns
     * {@code "1.235 ms"}.
     */
    public String toString(int significantDigits) {
        long nanos = elapsedNanos();

        TimeUnit unit = chooseUnit(nanos);
        double value = (double) nanos / NANOSECONDS.convert(1, unit);

        // Too bad this functionality is not exposed as a regular method call
        return String.format("%." + significantDigits + "g %s", value, abbreviate(unit));
    }

    private static TimeUnit chooseUnit(long nanos) {
        if (SECONDS.convert(nanos, NANOSECONDS) > 0) {
            return SECONDS;
        }
        if (MILLISECONDS.convert(nanos, NANOSECONDS) > 0) {
            return MILLISECONDS;
        }
        if (MICROSECONDS.convert(nanos, NANOSECONDS) > 0) {
            return MICROSECONDS;
        }
        return NANOSECONDS;
    }

    private static String abbreviate(TimeUnit unit) {
        switch (unit) {
        case NANOSECONDS:
            return "ns";
        case MICROSECONDS:
            return "\u03bcs"; // μs
        case MILLISECONDS:
            return "ms";
        case SECONDS:
            return "s";
        default:
            throw new AssertionError();
        }
    }
}
