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
import psychopath.File;

class LicenseTaskTest extends AbstractTaskTest {

    @Test
    void writeHeader() {
        project.set(bee.api.License.MIT);
        project.source("A");

        File source = project.getInput().file("main/java/A.java");
        assert source.isPresent();
        assert !source.text().contains("MIT License");

        Task.by(License.class).update();

        assert source.text().contains("MIT License");
    }
}
