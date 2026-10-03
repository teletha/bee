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

class EclipseTest extends AbstractTaskTest {

    @Test
    void name() {
        assert Task.by(Eclipse.class).name().equals("Eclipse");
    }

    @Test
    void exist() {
        Eclipse eclipse = Task.by(Eclipse.class);
        assert !eclipse.exist(project);

        project.getRoot().file(".classpath").create();

        assert eclipse.exist(project);
    }

    @Test
    void delete() {
        File classpath = project.getRoot().file(".classpath").create();
        File projectFile = project.getRoot().file(".project").create();
        File factorypath = project.getRoot().file(".factorypath").create();
        Directory settings = project.getRoot().directory(".settings").create();

        assert classpath.isPresent();
        assert projectFile.isPresent();
        assert factorypath.isPresent();
        assert settings.isPresent();

        Task.by(Eclipse.class).delete();

        assert classpath.isAbsent();
        assert projectFile.isAbsent();
        assert factorypath.isAbsent();
        assert settings.isAbsent();
    }
}
