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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>
 * Concurrent test utility.
 * </p>
 * 
 * @version 2011/03/31 17:53:21
 */
public class ManyWorlds extends ReusableRule {

    /** Thread pool for test. */
    private ExecutorService pool = Executors.newFixedThreadPool(8);

    /**
     * @see testament.ReusableRule#afterClass()
     */
    @Override
    protected void afterClass() {
        pool.shutdown();
    }

}
