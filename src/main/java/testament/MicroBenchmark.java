/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package testament;

import static java.math.BigInteger.*;
import static org.junit.Assert.*;

import java.math.BigInteger;
import java.text.DecimalFormat;

import kiss.I;

/**
 * <p>
 * Micro benchmark test on Junit4.
 * </p>
 * <p>
 * When writing a microbenchmark to measure the performance of a language primitive like
 * synchronization, you're grappling with the Heisenberg principle. You want to measure how fast
 * operation X is, so you don't want to do anything else besides X. But often, the result is a
 * do-nothing benchmark, which the compiler can optimize away partially or completely without you
 * realizing it, making the test run faster than expected. If you put extraneous code Y into your
 * benchmark, you're now measuring the performance of X+Y, introducing noise into your measurement
 * of X, and worse, the presence of Y changes how the JIT will optimize X. Writing a good
 * microbenchmark means finding that elusive balance between not enough filler and dataflow
 * dependency to prevent the compiler from optimizing away your entire program, and so much filler
 * that what you're trying to measure gets lost in the noise.
 * </p>
 * <p>
 * Because runtime compilation uses profiling data to guide its optimization, the JIT may well
 * optimize the test code differently than it would real code. As with all benchmarks, there is a
 * significant risk that the compiler will be able to optimize away the whole thing, because it will
 * (correctly) realize that the benchmark code doesn't actually do anything or produce a result that
 * is used for anything. Writing effective benchmarks requires that we "fool" the compiler into not
 * pruning away code as dead, even though it really is. The use of the counter variables in the
 * Incrementer classes is a failed attempt to fool the compiler, but compilers are often smarter
 * than we give them credit for when it comes to eliminating dead code.
 * </p>
 * 
 * @version 2010/09/30 11:26:39
 */
public final class MicroBenchmark<T> {

    /** 2 */
    static final BigInteger TWO = new BigInteger("2");

    /** 1,000 */
    static final BigInteger K = new BigInteger("1000");

    /** 1,000,000 */
    static final BigInteger M = new BigInteger("1000000");

    /** 1,000,000,000 */
    static final BigInteger G = new BigInteger("1000000000");

    /** The task to estimate execution time. */
    private final BenchmarkCode task;

    /** The frequency of measurement. */
    private final int frequency;

    /** The time of JVM warmup. (unit: second) */
    private final int warmup;

    /** The threshold of measurement time. (unit: ns) */
    private final BigInteger threshold = new BigInteger("1").multiply(G);

    /**
     * Create Benchmark instance.
     * 
     * @param task A task to benchmark.
     * @param frequency A frequency of measurement.
     */
    public MicroBenchmark(BenchmarkCode task) {
        this(task, 30);
    }

    /**
     * Create Benchmark instance.
     * 
     * @param task A task to benchmark.
     * @param frequency A frequency of measurement.
     */
    public MicroBenchmark(BenchmarkCode task, int frequency) {
        this(task, frequency, 5);
    }

    /**
     * Create Benchmark instance.
     * 
     * @param task A task to benchmark.
     * @param frequency A frequency of measurement.
     * @param warmup A time of JVM warmup. (unit: second)
     */
    public MicroBenchmark(BenchmarkCode task, int frequency, int warmup) {
        this.task = task;
        this.frequency = frequency;
        this.warmup = warmup;

        if (frequency < 5) {
            fail("There is too few measurement number of times. (minimus is 10)");
        }

        if (100 <= frequency) {
            fail("There is too many measurement number of times. (maximum is 99)");
        }
    }

    /**
     * Execute benchmarck test and report it.
     */
    public MeasurementResult execute() {
        MeasurementResult first = measure(1);
        assertNotNull("Benckmark task must return not null but something.", first.hash);
        assertEquals("Benchmark task must be able to execute within 1 second.", -1, first.time.compareTo(threshold));

        // warmup JVM
        System.out.println("JVM is warming up now...");

        for (long count = 1, start = System.currentTimeMillis(); System.currentTimeMillis() - start < 1000 * warmup; count *= 2) {
            measure(count);
        }

        // decided the number of executions
        BigInteger frequency = ONE;

        while (true) {
            MeasurementResult result = measure(frequency.longValue());

            if (result.time.compareTo(threshold) == -1) {
                frequency = frequency.multiply(TWO);
            } else {
                frequency = frequency.multiply(G).divide(result.time);
                break;
            }
        }

        // measure actually
        DecimalFormat counterFormat = new DecimalFormat("00");

        MeasurementResult[] results = new MeasurementResult[this.frequency];

        for (int i = 0; i < this.frequency; i++) {
            results[i] = measure(frequency.longValue());
            System.out.println(counterFormat.format(i + 1) + " : " + results[i]);
        }

        // prevent dead-code-elimination

        // report
        MeasurementResult total = new MeasurementResult(0, 0, 0);

        for (MeasurementResult result : results) {
            total = total.add(result);
        }
        System.out.println("sum : " + total);
        return total;
    }

    /**
     * Measures the execution time of <code>frequency</code> calls of the specified task.
     */
    private MeasurementResult measure(long frequency) {
        try {
            // reduce dead-code-elimination
            int hash = 0;

            // measure actually
            long start = System.nanoTime();
            for (long i = 0; i < frequency; i++) {
                hash ^= task.call().hashCode();
            }
            long end = System.nanoTime();

            // calculate execution time
            return new MeasurementResult(frequency, end - start, hash);
        } catch (Throwable e) {
            throw I.quiet(e);
        }
    }
}
