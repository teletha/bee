/*
 * Copyright (C) 2025 The BEE Development Team
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
import bee.TaskOperations;
import psychopath.Directory;
import psychopath.File;

class JarTest extends AbstractTaskTest {

    @Test
    void jarMainSource() {
        project.source("A");
        project.source("test.B");
        project.resource("C");

        File createdJar = project.locateJar();

        assert createdJar.isAbsent();

        Jar task = Task.by(Jar.class);
        task.source();

        assert createdJar.isPresent();
        assert createdJar.size() != 0;

        Directory unpacked = createdJar.unpackToTemporary();
        assert unpacked.file("A.class").isPresent();
        assert unpacked.file("test/B.class").isPresent();
        assert unpacked.file("C").isPresent();
    }

    @Test
    void modifyClass() {
        try {
            project.source("AA");
            project.source("test.B");
            project.resource("C");
            project.resource("test/D");

            File createdJar = project.locateJar();

            assert createdJar.isAbsent();

            TaskOperations.config(Jar.class, conf -> {
                conf.removeTraceInfo = true;
            });

            Jar task = Task.by(Jar.class);
            task.source();

            assert createdJar.isPresent();
            assert createdJar.size() != 0;

            Directory unpacked = createdJar.unpackToTemporary();
            assert unpacked.file("AA.class").isPresent();
            assert unpacked.file("test/B.class").isPresent();
            assert unpacked.file("C").isPresent();
            assert unpacked.file("test/D").isPresent();
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }
}