/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Deque;

import bee.api.Command;

class CommandLineUserInterface extends UserInterface {

    /** The original standard output. */
    private final PrintStream standardOutput;

    /** The original standard error. */
    private final PrintStream standardError;

    /** The task state. */
    private boolean first = false;

    /** The command queue. */
    private Deque<String> commands = new ArrayDeque();

    /** The view state. */
    private boolean eraseNextLine = false;

    /**
     * 
     */
    CommandLineUserInterface() {
        standardOutput = System.out;
        standardError = System.err;

        System.setOut(new Delegator());
        System.setErr(new Delegator());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void startCommand(String name, Command command) {
        first = true;
        commands.add(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void endCommand(String name, Command command) {
        if (first) {
            showCommandName();
        }
        first = true;
        standardOutput.print(Platform.EOL);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected synchronized void write(String message) {
        if (first) {
            showCommandName();
            first = false;
        }

        if (eraseNextLine) {
            message = "[2K" + message;
        }
        eraseNextLine = message.endsWith("\r");

        if (eraseNextLine) {
            standardOutput.print(message);
        } else {
            standardOutput.println(message);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Appendable getInterface() {
        if (first) {
            showCommandName();
            first = false;
        }
        return standardOutput;
    }

    /**
     * <p>
     * Show command name.
     * </p>
     */
    private void showCommandName() {
        String command = commands.pollLast();

        if (command != null) {
            standardOutput.println("◆ " + command.replace(":", " : ") + " ◆");
        }
    }

    /**
     * Delgator for UI.
     */
    private class Delegator extends PrintStream {

        /**
         * 
         */
        public Delegator() {
            super(new ByteArrayOutputStream(), true, Platform.Encoding);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write(byte[] b) throws IOException {
            write(String.valueOf(b));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void flush() {
            standardOutput.flush();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() {
            standardOutput.close();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean checkError() {
            return standardOutput.checkError();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write(int b) {
            write(String.valueOf(b));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write(byte[] buf, int off, int len) {
            write(new String(buf, off, len));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void print(boolean b) {
            write(String.valueOf(b));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void print(char c) {
            write(String.valueOf(c));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void print(int i) {
            write(String.valueOf(i));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void print(long l) {
            write(String.valueOf(l));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void print(float f) {
            write(String.valueOf(f));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void print(double d) {
            write(String.valueOf(d));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void print(char[] s) {
            write(String.valueOf(s));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void print(String s) {
            write(s);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void print(Object obj) {
            write(String.valueOf(obj));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void println() {
            write(Platform.EOL);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void println(boolean x) {
            write(String.valueOf(x));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void println(char x) {
            write(String.valueOf(x));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void println(int x) {
            write(String.valueOf(x));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void println(long x) {
            write(String.valueOf(x));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void println(float x) {
            write(String.valueOf(x));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void println(double x) {
            write(String.valueOf(x));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void println(char[] x) {
            write(String.valueOf(x));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void println(String x) {
            write(x);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void println(Object x) {
            write(String.valueOf(x));
        }
    }
}
