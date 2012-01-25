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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * <p>
 * This is pseudo character-based user.
 * </p>
 * 
 * @version 2011/09/22 15:34:02
 */
public class CommandLineUser extends ReusableRule {

    /** The mock system input. */
    private MockInputStream input;

    /** The original system output. */
    private MockOutputStream output;

    /** The original system error. */
    private MockOutputStream error;

    /** The ignore system output. */
    private boolean ignore;

    /**
     * 
     */
    public CommandLineUser() {
        this(false);
    }

    /**
     * @param ignoreOutput
     */
    public CommandLineUser(boolean ignoreOutput) {
        this.ignore = ignoreOutput;
    }

    /**
     * @see testament.ReusableRule#before(java.lang.reflect.Method)
     */
    @Override
    protected void before(Method method) throws Exception {
        // swap
        System.setIn(input = new MockInputStream());

        if (!ignore) {
            System.setOut(output = new MockOutputStream(false));
            System.setErr(error = new MockOutputStream(true));
        }
    }

    /**
     * @see testament.ReusableRule#after(java.lang.reflect.Method)
     */
    @Override
    protected void after(Method method) {
        // restore original
        System.setIn(input.original);

        if (!ignore) {
            System.setOut(output.original);
            System.setErr(error.original);
        }
    }

    /**
     * @param value
     */
    public void willInput(String... values) {
        for (String value : values) {
            input.deque.add(new UserInput(value.concat("\r\n")));
        }
    }

    /**
     * @version 2011/09/22 16:12:01
     */
    private static class MockOutputStream extends PrintStream {

        /** The original. */
        private final PrintStream original;

        /**
         * @param original
         */
        private MockOutputStream(boolean error) {
            super(error ? System.err : System.out);

            this.original = (PrintStream) out;
        }

        /**
         * @see java.io.PrintStream#write(byte[], int, int)
         */
        @Override
        public void write(byte[] buf, int off, int len) {
            // do nothing
        }
    }

    /**
     * @version 2011/09/22 15:36:26
     */
    private static class MockInputStream extends InputStream {

        /** The original system input. */
        private final InputStream original = System.in;

        /** The user input. */
        private final Deque<UserInput> deque = new ArrayDeque();

        /**
         * @see java.io.InputStream#read()
         */
        @Override
        public int read() throws IOException {
            UserInput input = deque.peekFirst();

            if (input == null) {
                return -1;
            } else {
                int i = input.read();

                if (i == -1) {
                    deque.pollFirst();

                    return -1;
                }
                return i;
            }
        }
    }

    /**
     * @version 2011/09/22 15:38:07
     */
    private static class UserInput {

        /** The user input value. */
        private final String input;

        /** The current input index. */
        private int index = 0;

        /**
         * @param input
         */
        private UserInput(String input) {
            this.input = input;
        }

        /**
         * <p>
         * Read next input character.
         * </p>
         * 
         * @return A next input character.
         */
        private int read() {
            return index == input.length() ? -1 : input.charAt(index++);
        }
    }
}
