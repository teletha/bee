/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import org.junit.Before;

import bee.BlinkProject;
import kiss.I;

/**
 * @version 2017/01/19 11:14:17
 */
public abstract class TaskTestBase {

    /** The base project. */
    protected BlinkProject project;

    @Before
    public void before() {
        project = I.make(BlinkProject.class);
    }
}
