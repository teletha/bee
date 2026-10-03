/*
 * Copyright (C) 2026 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import org.junit.jupiter.api.Test;

import bee.AbstractTaskTest;
import bee.Task;
import psychopath.Directory;
import psychopath.File;

class CleanTest extends AbstractTaskTest {

    @Test
    void deleteOutputExceptJar() {
        Directory output = project.getOutput().create();
        File source = output.file("A.txt").create();
        File jar = output.file("A.jar").create();

        assert source.isPresent();
        assert jar.isPresent();

        Task.by(Clean.class).all();

        assert source.isAbsent();
        assert jar.isPresent();
    }

    @Test
    void cleanAbsentOutput() {
        // Cleaning a project which has no output directory should not fail.
        Task.by(Clean.class).all();
    }
}
