/*
 * Copyright (C) 2022 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import bee.TaskTestBase;
import psychopath.File;

public class CompileTest extends TaskTestBase {

    @Test
    public void compileMainSource() throws Exception {
        project.source("A");
        project.source("test.B");
        project.resource("C");

        File A = project.locateMainOutput("A.class");
        File B = project.locateMainOutput("test/B.class");
        File C = project.locateMainOutput("C");

        assert A.isAbsent();
        assert B.isAbsent();
        assert C.isAbsent();

        Compile compile = new Compile();
        compile.source();

        assert A.isPresent();
        assert B.isPresent();
        assert C.isPresent();
    }

    @Test
    public void compileTestSource() throws Exception {
        project.sourceTest("A");
        project.sourceTest("test.B");
        project.resourceTest("C");

        File A = project.locateTestOutput("A.class");
        File B = project.locateTestOutput("test/B.class");
        File C = project.locateTestOutput("C");

        assert A.isAbsent();
        assert B.isAbsent();
        assert C.isAbsent();

        Compile compile = new Compile();
        compile.test();

        assert A.isPresent();
        assert B.isPresent();
        assert C.isPresent();
    }

    @Test
    public void compileProjectSource() throws Exception {
        project.sourceProject("A");
        project.sourceProject("test.B");
        project.resourceProject("C");

        Path A = project.locateProjectOutput("A.class");
        Path B = project.locateProjectOutput("test/B.class");
        Path C = project.locateProjectOutput("C");

        assert Files.notExists(A);
        assert Files.notExists(B);
        assert Files.notExists(C);

        Compile compile = new Compile();
        compile.project();

        assert Files.exists(A);
        assert Files.exists(B);
        assert Files.exists(C);
    }

    @Test
    public void compileInvalidSource() throws Exception {
        Assertions.assertThrows(Throwable.class, () -> {
            project.source("A", "invalid");

            Compile compile = new Compile();
            compile.source();
        });
    }
}