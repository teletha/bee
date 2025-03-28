/*
 * Copyright (C) 2025 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.util;

import javax.lang.model.SourceVersion;

import org.junit.jupiter.api.Test;

import bee.BlinkProject;
import bee.sample.Interface;
import psychopath.File;

class JavaCompilerTest {

    @Test
    void outputPresent() {
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
    void outputAbsent() {
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

    @Test
    void ecj() {
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
                .setEclipseCompiler(true)
                .setVersion(SourceVersion.RELEASE_16)
                .compile();

        assert source.isPresent();
        assert bytecode.isPresent();
    }
}