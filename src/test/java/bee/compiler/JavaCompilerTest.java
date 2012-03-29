/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.compiler;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;

import bee.Null;
import bee.compiler.source01.MainClass;

/**
 * @version 2010/12/19 10:21:32
 */
public class JavaCompilerTest {

    @Rule
    public static final PrivateSourceDirectory source01 = new PrivateSourceDirectory("source01");

    @Test
    public void outputPresent() throws Exception {
        Path source = source01.locateSourceCode(MainClass.class);
        Path bytecode = source01.locateByteCode(MainClass.class);
        assertTrue(Files.exists(source));
        assertTrue(Files.notExists(bytecode));

        Files.createDirectories(source01.output);
        assertTrue(Files.exists(source01.output));

        JavaCompiler compiler = new JavaCompiler(Null.UI);
        compiler.addSourceDirectory(source01.root);
        compiler.setOutput(source01.output);
        compiler.compile();

        assertTrue(Files.exists(source));
        assertTrue(Files.exists(bytecode));
    }

    @Test
    public void outputAbsent() throws Exception {
        Path source = source01.locateSourceCode(MainClass.class);
        Path bytecode = source01.locateByteCode(MainClass.class);
        assertTrue(Files.exists(source));
        assertTrue(Files.notExists(bytecode));

        Files.deleteIfExists(source01.output);
        assertTrue(Files.notExists(source01.output));

        JavaCompiler compiler = new JavaCompiler(Null.UI);
        compiler.addSourceDirectory(source01.root);
        compiler.setOutput(source01.output);
        compiler.compile();

        assertTrue(Files.exists(source));
        assertTrue(Files.exists(bytecode));
    }
}
