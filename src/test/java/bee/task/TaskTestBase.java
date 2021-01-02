/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import org.junit.jupiter.api.BeforeEach;

import bee.BlinkProject;
import kiss.I;

/**
 * @version 2018/03/31 21:58:57
 */
public abstract class TaskTestBase {

    /** The base project. */
    protected BlinkProject project;

    @BeforeEach
    public void before() {
        project = I.make(BlinkProject.class);
    }
}