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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import bee.TaskTestBase;
import psychopath.Directory;
import psychopath.File;

@Execution(ExecutionMode.SAME_THREAD)
class JarTest extends TaskTestBase {

    @Test
    void jarMainSource() {
        System.out.println("JaMainSource " + System.identityHashCode(project));
        project.source("A");
        project.source("test.B");
        project.resource("C");

        File createdJar = project.locateJar();

        assert createdJar.isAbsent();

        Jar task = new Jar();
        task.source();

        System.out.println(project.getClasses());
        project.getClasses().walkFile().to(e -> {
            System.out.println("JarMain " + e);
        });

        assert createdJar.isPresent();
        assert createdJar.size() != 0;

        Directory unpacked = createdJar.unpackToTemporary();
        assert unpacked.file("A.class").isPresent();
        assert unpacked.file("test/B.class").isPresent();
        assert unpacked.file("C").isPresent();
    }

    @Test
    void modifyClass() {
        System.out.println("modifyClass " + System.identityHashCode(project));
        project.source("AA");
        project.source("test.B");
        project.resource("C");

        File createdJar = project.locateJar();

        assert createdJar.isAbsent();

        Jar task = new Jar();
        task.removeTraceInfo = true;
        task.source();

        System.out.println(project.getClasses());
        project.getClasses().walkFile().to(e -> {
            System.out.println("Modified " + e);
        });

        assert createdJar.isPresent();
        assert createdJar.size() != 0;

        Directory unpacked = createdJar.unpackToTemporary();
        assert unpacked.file("AA.class").isPresent();
        assert unpacked.file("test/B.class").isPresent();
        assert unpacked.file("C").isPresent();
    }
}