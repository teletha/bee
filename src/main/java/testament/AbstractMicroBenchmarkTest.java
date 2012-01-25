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

/**
 * @version 2011/12/29 12:58:23
 */
public abstract class AbstractMicroBenchmarkTest {

    public void benchmark(BenchmarkCode task) {
        MicroBenchmark benchmark = new MicroBenchmark(task, 30, 10);
        benchmark.execute();
    }
}
