/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package testament;

import java.io.File;
import java.io.FileReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.AccessControlException;

import org.junit.Rule;
import org.junit.Test;

import kiss.I;

/**
 * @version 2010/02/09 11:23:23
 */
@SuppressWarnings("resource")
public class SandboxTest {

    @Rule
    public static final Sandbox sandbox = new Sandbox(Sandbox.READ);

    @Test
    public void read1() throws Exception {
        sandbox.readable(true);

        new FileReader(new File("pom.xml"));
    }

    @Test(expected = AccessControlException.class)
    public void read2() throws Exception {
        new FileReader(new File("pom.xml"));
    }

    @Test
    public void writableFile() throws Exception {
        Path file = I.locateTemporary();

        sandbox.writable(false, file);

        // try to write
        Writer writer = null;

        try {
            writer = Files.newBufferedWriter(file, Charset.defaultCharset());

            throw new AssertionError("This is writable file.");
        } catch (AccessControlException e) {
            // success
        } finally {
            I.quiet(writer);
        }

        // make writable
        sandbox.writable(true, file);

        try {
            writer = Files.newBufferedWriter(file, Charset.defaultCharset());
        } catch (AccessControlException e) {
            throw new AssertionError("This is unwritable file.");
        } finally {
            I.quiet(writer);
        }
    }

    @Test(expected = NoSuchFileException.class)
    public void writableDirectory() throws Exception {
        Path file = I.locateTemporary();

        sandbox.writable(false, file);

        // try to write
        Writer writer = null;

        try {
            writer = Files.newBufferedWriter(file.resolve("file"), Charset.defaultCharset());

            throw new AssertionError("This is writable file.");
        } catch (AccessControlException e) {
            // success
        }

        // make writable
        sandbox.writable(true, file);

        try {
            writer = Files.newBufferedWriter(file.resolve("file"), Charset.defaultCharset());
        } catch (AccessControlException e) {
            throw new AssertionError("This is unwritable file.");
        } finally {
            I.quiet(writer);
        }
    }
}
