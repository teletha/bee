/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;

import bee.BlinkProject;

/**
 * @version 2012/04/02 9:37:56
 */
public class CompileTest {

    @Rule
    public final BlinkProject project = new BlinkProject() {

        {
            source("A");
            source("test.B");
            resource("C");
        }
    };

    @Test
    public void compile() throws Exception {
        Path A = project.locateClass("A.class");
        Path B = project.locateClass("test/B.class");
        Path C = project.locateClass("C");

        assert Files.notExists(A);
        assert Files.notExists(B);
        assert Files.notExists(C);

        Compile compile = new Compile();
        compile.source();

        assert Files.exists(A);
        assert Files.exists(B);
        assert Files.exists(C);
    }
}
