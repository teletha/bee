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

import org.junit.After;
import org.junit.Before;

/**
 * DOCUMENT.
 * 
 * @version 2008/06/01 14:37:08
 */
public abstract class MultiThreadTestCase {

    /** The thread pool. */
    protected ExecutorService executor;

    /**
     * Initialize all resources.
     */
    @Before
    public void init() throws Exception {
        // create new thread pool
        executor = Executors.newCachedThreadPool();

    }

    /**
     * Release all resources.
     */
    @After
    public void release() throws Exception {
        // shutdown all pooled threads
        executor.shutdownNow();
    }
}
