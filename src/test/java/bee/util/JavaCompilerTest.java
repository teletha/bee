/*
 * Copyright (C) 2018 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.util;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import bee.BlinkProject;
import bee.sample.Interface;

/**
 * @version 2018/03/31 16:54:28
 */
public class JavaCompilerTest {

    @Test
    public void outputPresent() throws Exception {
        BlinkProject project = new BlinkProject();
        Path source = project.importBy(Interface.class);
        Path bytecode = project.locateByteCode(Interface.class);

        assert Files.exists(source);
        assert Files.notExists(bytecode);

        Files.createDirectories(project.getClasses());
        assert Files.exists(project.getClasses());

        JavaCompiler compiler = new JavaCompiler();
        compiler.addSourceDirectory(project.getSourceSet().asTemporary());
        compiler.setOutput(project.getClasses());
        compiler.compile();

        assert Files.exists(source);
        assert Files.exists(bytecode);
    }

    @Test
    public void outputAbsent() throws Exception {
        BlinkProject project = new BlinkProject();
        Path source = project.importBy(Interface.class);
        Path bytecode = project.locateByteCode(Interface.class);

        assert Files.exists(source);
        assert Files.notExists(bytecode);

        Files.deleteIfExists(project.getClasses());
        assert Files.notExists(project.getClasses());

        JavaCompiler compiler = new JavaCompiler();
        compiler.addSourceDirectory(project.getSourceSet().asTemporary());
        compiler.setOutput(project.getClasses());
        compiler.compile();

        assert Files.exists(source);
        assert Files.exists(bytecode);
    }
}
