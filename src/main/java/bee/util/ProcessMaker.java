/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import kiss.I;
import bee.Platform;

/**
 * <p>
 * Utility for creating sub process.
 * </p>
 * 
 * @version 2012/04/05 11:50:55
 */
public class ProcessMaker {

    /** The working directory. */
    private Path directory;

    /**
     * <p>
     * Set the working directory of the sub process.
     * </p>
     * 
     * @param directory A working directory.
     * @return Fluent API.
     */
    public ProcessMaker setWorkingDirectory(Path directory) {
        this.directory = directory;

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

            Process process = builder.start();

            new ProcessReader(process.getInputStream(), System.out).start();
            new ProcessReader(process.getErrorStream(), System.err).start();

            int result = process.waitFor();

            if (result != 0) {
                // error
            }

            process.destroy();
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    /**
     * @version 2012/04/05 12:58:08
     */
    private static class ProcessReader extends Thread {

        /** The input. */
        private final InputStreamReader input;

        /** The output. */
        private final PrintStream output;

        /**
         * @param input
         * @param output
         */
        private ProcessReader(InputStream input, PrintStream output) {
            this.input = new InputStreamReader(input, Platform.Encoding);
            this.output = output;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public synchronized void start() {
            try {
                int i = -1;

                do {
                    // wait next input
                    input.ready();

                    // read next character
                    i = input.read();

                    // outpu it
                    output.append((char) i);
                } while (i != -1);
            } catch (IOException e) {
                throw I.quiet(e);
            } finally {
                I.quiet(input);
                // don't close output
            }
        }
    }
}
