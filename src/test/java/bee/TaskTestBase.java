/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import bee.Task.Cache;
import kiss.I;
import psychopath.File;

public abstract class TaskTestBase {

    static {
        I.load(Bee.class);
    }

    /** The base project. */
    protected BlinkProject project;

    /** The NoOP task. */
    protected Task noop;

    @BeforeEach
    public void before() {
        project = I.make(BlinkProject.class);
        noop = I.make(NoOPTask.class);

        LifestyleForProject.local.get().set(project);
        System.out.println("Set project " + System.identityHashCode(project));
    }

    @AfterEach
    public void after() {
        project.associate(Cache.class).clear();
        // LifestyleForProject.local.get().set(null);
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