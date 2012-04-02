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

import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;

import bee.definition.PrivateProject;
import bee.definition.TemporaryProjectBuilder;
import bee.projects.noconfig.project.java.NoConfigProject;

/**
 * @version 2012/04/02 9:37:56
 */
public class CompileTest {

    @Rule
    public static final TemporaryProjectBuilder temporary = new TemporaryProjectBuilder(NoConfigProject.class);

    @Test
    public void compile() throws Exception {
        Compile compile = new Compile();
        compile.source();
    }

    /**
     * @version 2012/04/02 11:59:54
     */
    private static class TestProject extends PrivateProject {

        private Path path = null;

        {

        }

    }
}
