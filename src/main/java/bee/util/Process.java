/*
 * Copyright (C) 2015 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import bee.Platform;
import bee.UserInterface;
import kiss.I;

/**
 * <p>
 * Utility for creating sub process.
 * </p>
 * 
 * @version 2016/07/01 15:10:31
 */
public class Process {

    /** The working directory. */
    private Path directory;

    /** {@link System#out} and {@link System#in} encoding. */
    private Charset encoding;

    /** The execution type. */
    private boolean sync = true;

    /** The flag. */
    private boolean showOutput = true;

    /**
     * Hide Constructor.
     */
    private Process() {
    }

    /**
     * <p>
     * Create new {@link Process} builder.
     * </p>
     * 
     * @return
     */
    public static Process with() {
        return new Process();
    }

    /**
     * <p>
     * Set the working directory of the sub process.
     * </p>
     * 
     * @param directory A working directory.
     * @return Fluent API.
     */
    public Process workingDirectory(Path directory) {
        this.directory = directory;

        // API definition
        return this;
    }

    /**
     * <p>
     * Set {@link System#out} and {@link System#in} encoding. .
     * </p>
     * 
     * @param encoding
     * @return
     */
    public Process encoding(Charset encoding) {
        this.encoding = encoding;

        // API definition
        return this;
    }

    /**
     * <p>
     * Make this process running asynchronously.
     * </p>
     * 
     * @return Fluent API.
     */
    public Process inParallel() {
        this.sync = false;

        // API definition
        return this;
    }

    /**
     * <p>
     * Make this process ignore system output.
     * </p>
     * 
     * @return Fluent API.
     */
    public Process ignoreOutput() {
        this.showOutput = false;

        // API definition
        return this;
    }

    /**
     * <p>
     * Execute sub process.
     * </p>
     * 
     * @param command
     */
    public void run(List<String> command) {
        try {
            ProcessBuilder builder = new ProcessBuilder();

            if (encoding == null) {
                encoding = Platform.Encoding;
            }

            if (directory == null) {
                directory = I.locateTemporary();
            }

            if (Files.notExists(directory)) {
                directory = Files.createDirectories(directory);
            }

            if (!Files.isDirectory(directory)) {
                directory = directory.getParent();
            }
            builder.command(command);

            java.lang.Process process = builder.start();

            if (showOutput) {
                new ProcessReader(new InputStreamReader(process.getInputStream(), encoding)).start();
                new ProcessReader(new InputStreamReader(process.getErrorStream(), encoding)).start();
            }

            if (sync) {
                process.waitFor();
            }

            process.destroy();
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    /**
     * @version 2015/06/18 1:55:33
     */
    private static class ProcessReader extends Thread {

        /** The input. */
        private final InputStreamReader input;

        /** The output. */
        private final Appendable output;

        /**
         * @param input
         */
        private ProcessReader(InputStreamReader input) {
            this.input = input;
            this.output = I.make(UserInterface.class).getInterface();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public synchronized void start() {
            try {
                int i = input.read();

                while (i != -1) {
                    // output it
                    output.append((char) i);

                    // read next character
                    i = input.read();
                }
            } catch (IOException e) {
                throw I.quiet(e);
            } finally {
                I.quiet(input);
                // don't close output
                // I.quiet(output);
            }
        }
    }
}
