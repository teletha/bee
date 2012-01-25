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

import java.math.BigInteger;
import java.text.DecimalFormat;

/**
 * DOCUMENT.
 * 
 * @version 2008/11/05 15:16:11
 */
public class MeasurementResult {

    /** The frequency of measurement. */
    public final BigInteger frequency;

    /** The measurement time. */
    public final BigInteger time;

    /** The measurement time per one execution of the specified task. */
    public final BigInteger timesPerExecution;

    /** The number of task executions per one second. */
    public final BigInteger executionsPerSecond;

    /** The hash of this result. */
    public final int hash;

    /***
     * Create MeasurementResult instance.
     * 
     * @param frequency
     * @param time
     * @param hash
     */
    MeasurementResult(long frequency, long time, int hash) {
        this(new BigInteger(String.valueOf(frequency)), new BigInteger(String.valueOf(time)), hash);
    }

    /**
     * Create MeasurementResult instance.
     * 
     * @param frequency
     * @param time
     * @param hash
     */
    MeasurementResult(BigInteger frequency, BigInteger time, int hash) {
        this.frequency = frequency;
        this.time = time;
        this.timesPerExecution = (frequency.equals(ZERO)) ? ZERO : time.divide(frequency);
        this.executionsPerSecond = (time.equals(ZERO)) ? ZERO : frequency.multiply(MicroBenchmark.G).divide(time);
        this.hash = hash;
    }

    /**
     * For report.
     * 
     * @param result
     * @return
     */
    MeasurementResult add(MeasurementResult result) {
        return new MeasurementResult(result.frequency.add(frequency), result.time.add(time), result.hash + hash);
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return hash;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        DecimalFormat format = new DecimalFormat();

        StringBuilder builder = new StringBuilder();
        builder.append(format.format(time.divide(MicroBenchmark.M)));
        builder.append("ms  ");
        builder.append(format.format(timesPerExecution));
        builder.append("ns/e  ");
        builder.append(format.format(executionsPerSecond));
        builder.append("e/s");

        return builder.toString();
    }
}
