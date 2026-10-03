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

class PrototypeTest extends AbstractTaskTest {

    @Test
    void createJavaProjectSkeleton() {
        assert project.getInput().directory("main/java/test").isAbsent();

        Task.by(Prototype.class).java();

        // The package directory is derived from the project group ("test").
        assert project.getInput().directory("main/java/test").isPresent();
        assert project.getInput().directory("main/resources/test").isPresent();
        assert project.getInput().directory("test/java/test").isPresent();
        assert project.getInput().directory("test/resources/test").isPresent();
    }
}
