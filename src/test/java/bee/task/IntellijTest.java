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

class IntellijTest extends AbstractTaskTest {

    @Test
    void name() {
        assert Task.by(Intellij.class).name().equals("IntelliJ IDEA");
    }

    @Test
    void exist() {
        Intellij intellij = Task.by(Intellij.class);
        assert !intellij.exist(project);

        project.getRoot().directory(".idea").create().file("modules.xml").create();

        assert intellij.exist(project);
    }

    @Test
    void delete() {
        Directory idea = project.getRoot().directory(".idea").create();
        idea.file("modules.xml").create();
        assert idea.isPresent();

        Task.by(Intellij.class).delete();

        assert idea.isAbsent();
    }
}
