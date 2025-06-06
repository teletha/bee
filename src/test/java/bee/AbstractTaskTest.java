/*
 * Copyright (C) 2025 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee;

import org.junit.jupiter.api.BeforeEach;

import psychopath.File;

public abstract class AbstractTaskTest extends TaskOperations {

    protected final TestableProject project = new TestableProject();

    @BeforeEach
    public void before() {
        ForProject.local.set(project);
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
}