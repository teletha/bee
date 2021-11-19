/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.util;

import org.junit.jupiter.api.Test;

import bee.BlinkProject;
import bee.sample.Interface;
import psychopath.File;

/**
 * @version 2018/03/31 16:54:28
 */
public class JavaCompilerTest {

    @Test
    public void outputPresent() throws Exception {
        BlinkProject project = new BlinkProject();
        File source = project.importBy(Interface.class);
        File bytecode = project.locateByteCode(Interface.class);

        assert source.isPresent();
        assert bytecode.isAbsent();

        project.getClasses().create();
        assert project.getClasses().isPresent();

        JavaCompiler.with() //
                .addSourceDirectory(project.getSourceSet())
                .setOutput(project.getClasses())
                .compile();

        assert source.isPresent();
        assert bytecode.isPresent();
    }

    @Test
    public void outputAbsent() throws Exception {
        BlinkProject project = new BlinkProject();
        File source = project.importBy(Interface.class);
        File bytecode = project.locateByteCode(Interface.class);

        assert source.isPresent();
        assert bytecode.isAbsent();

        project.getClasses().create();
        assert project.getClasses().isPresent();

        project.getClasses().delete();
        assert project.getClasses().isAbsent();

        JavaCompiler.with() //
                .addSourceDirectory(project.getSourceSet())
                .setOutput(project.getClasses())
                .compile();

        assert source.isPresent();
        assert bytecode.isPresent();
    }
}