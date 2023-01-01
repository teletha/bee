/*
 * Copyright (C) 2023 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee;

import org.junit.jupiter.api.BeforeEach;

import kiss.I;
import psychopath.File;

public abstract class TaskTestBase {

    /** The base project. */
    protected BlinkProject project;

    /** The NoOP task. */
    protected Task noop;

    @BeforeEach
    public void before() {
        project = I.make(BlinkProject.class);
        noop = I.make(NoOPTask.class);

        LifestyleForProject.local.set(project);
    }

    /**
     * Helper method to locate file.
     * 
     * @param path
     * @return
     */
    protected final File locateFile(String path) {
        return project.getRoot().file(path);
    }

    /**
     * No-Operation Task.
     */
    protected static class NoOPTask extends Task {
    }
}