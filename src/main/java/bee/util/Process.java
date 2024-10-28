/*
 * Copyright (C) 2024 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import bee.Platform;
import bee.UserInterface;
import kiss.I;
import psychopath.Directory;
import psychopath.Locator;

public class Process {

    /** The working directory. */
    private Directory directory;

    /** {@link System#out} and {@link System#in} encoding. */
    private Charset encoding;

    /** The execution type. */
    private boolean sync = true;

    /** The flag. */
    private boolean showOutput = true;

    /** The exit code. */
    private int exit;

    /**
     * Hide Constructor.
     */
    private Process() {
    }

    /**
     * Create new {@link Process} builder.
     * 
     * @return
     */
    public static Process with() {
        return new Process();
    }

    /**
     * Shorthand to Process.with().run(commands).
     * 
     * @param commands
     */
    public static void runWith(Object... commands) {
        with().run(commands);
    }

    /**
     * Shorthand to Process.with().read(commands).
     * 
     * @param commands
     */
    public static String readWith(Object... commands) {
        return with().read(commands);
    }

    /**
     * Set the working directory of the sub process.
     * 
     * @param directory A working directory.
     * @return Fluent API.
     */
    public Process workingDirectory(Directory directory) {
        this.directory = directory;

        // API definition
        return this;
    }

    /**
     * Set {@link System#out} and {@link System#in} encoding. .
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
     * Make this process running asynchronously.
     * 
     * @return Fluent API.
     */
    public Process inParallel() {
        this.sync = false;
        this.showOutput = false;

        // API definition
        return this;
    }

    /**
     * Make this process ignore system output.
     * 
     * @return Fluent API.
     */
    public Process ignoreOutput() {
        this.showOutput = false;

        // API definition
        return this;
    }

    /**
     * Execute sub process.
     * 
     * @param commands
     */
    public int run(Object... commands) {
        List<String> list = new ArrayList();

        for (Object command : commands) {
            list.add(String.valueOf(command));
        }
        return run(list);
    }

    /**
     * Execute sub process.
     * 
     * @param command
     */
    public int run(List<String> command) {
        run(command, true);

        return exit;
    }

    /**
     * Execute sub process and accept its result.
     * 
     * @param commands
     */
    public String read(Object... commands) {
        List<String> list = new ArrayList();

        for (Object command : commands) {
            list.add(String.valueOf(command));
        }
        return read(list);
    }

    /**
     * Execute sub process and accept its result.
     * 
     * @param command
     */
    public String read(List<String> command) {
        return run(command, false);
    }

    /**
     * Execute sub process.
     * 
     * @param command
     * @param userOutput
     * @return
     */
    private String run(List<String> command, boolean userOutput) {
        try {
            ProcessBuilder builder = new ProcessBuilder();

            if (encoding == null) {
                encoding = Platform.Encoding;
            }

            if (directory == null) {
                directory = Locator.temporaryDirectory();
            }

            if (directory.isAbsent()) {
                directory.create();
            }

            if (!directory.isDirectory()) {
                directory = directory.parent();
            }
            builder.redirectErrorStream(true);
            builder.command(command);

            java.lang.Process process = builder.start();
            Appendable output = new StringBuilder();

            if (userOutput) {
                try {
                    output = I.make(UserInterface.class).getInterface();
                } catch (Exception e) {
                    // ignore
                }
            }

            if (showOutput) {
                new ProcessReader(new InputStreamReader(process.getInputStream(), encoding), output);
            }

            if (sync || !userOutput) {
                exit = process.waitFor();
            }

            if (sync) {
                process.destroy();
            }
            return userOutput ? null : output.toString().trim();
        } catch (Throwable e) {
            throw new Error("Command " + command + " is failed.", e);
        }
    }

    /**
     * 
     */
    private static class ProcessReader extends Thread {

        /** The input. */
        private final InputStreamReader input;

        /** The output. */
        private Appendable output;

        /**
         * @param input A process output.
         * @param output Bee input.
         */
        private ProcessReader(InputStreamReader input, Appendable output) {
            this.input = input;
            this.output = output;

            // start thread immediately
            start();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            try {
                char[] buffer = new char[8192];

                int size = input.read(buffer);

                while (size != -1) {
                    output.append(new String(buffer, 0, size));

                    // read next character
                    size = input.read(buffer);
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