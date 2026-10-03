/*
 * Copyright (C) 2026 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.junit.jupiter.api.Test;

import antibug.CommandLineUser;
import bee.UserInterface.CommandLineUserInterface;
import bee.util.Terminal;

class CommandLineUserInterfaceTest {

    /** The escape sequence which terminals send when the user presses the up arrow key. */
    private static final String UP = "\u001b[A";

    /** The escape sequence which terminals send when the user presses the down arrow key. */
    private static final String DOWN = "\u001b[B";

    /** The enter key. */
    private static final String ENTER = "\r";

    private final CommandLineUser user = new CommandLineUser(true);

    private final UserInterface ui = new CommandLineUserInterface(user.output, user.error, user.input);

    @Test
    void input() {
        String expected = "expected value";

        user.willInput(expected);

        String answer = ui.ask("question");

        assert answer.equals(expected);
    }

    @Test
    void inputEmpty() {
        String expected = "expected value";

        user.willInput("");
        user.willInput(expected);

        String answer = ui.ask("question");

        assert answer.equals(expected);
    }

    @Test
    void inputWithDefault() {
        String expected = "expected value";

        user.willInput(expected);

        String answer = ui.ask("question", "woo hoo");

        assert answer.equals(expected);
    }

    @Test
    void inputEmptyWithDefault() {
        String expected = "expected value";

        user.willInput("");

        String answer = ui.ask("question", expected);

        assert answer.equals(expected);
    }

    @Test
    void inputInt() {
        user.willInput("1");
        assert ui.ask("question", 10) == 1;

        user.willInput("-1");
        assert ui.ask("question", 10) == -1;

        user.willInput(" +2 ");
        assert ui.ask("question", 10) == 2;

        user.willInput("");
        assert ui.ask("question", 10) == 10;
    }

    @Test
    void inputPath() {
        Path def = Path.of("default");

        user.willInput("path");
        assert ui.ask("question", def).equals(Path.of("path"));

        user.willInput("path/with/directory");
        assert ui.ask("question", def).equals(Path.of("path/with/directory"));

        user.willInput("   ");
        assert ui.ask("question", def).equals(def);
    }

    @Test
    void selectByNumberWithoutTerminal() {
        List<String> items = new ArrayList();
        items.add("one");
        items.add("two");
        items.add("three");

        // The test environment has no terminal, so the numbered input is used.
        user.willInput("1");
        assert ui.ask("question", items).equals("one");

        user.willInput("2");
        assert ui.ask("question", items).equals("two");

        user.willInput("3");
        assert ui.ask("question", items).equals("three");
    }

    @Test
    void selectByEnterKey() {
        assert select(ENTER, "one", "two", "three").equals("one");
    }

    @Test
    void selectByArrowKey() {
        assert select(DOWN + ENTER, "one", "two", "three").equals("two");

        assert select(DOWN + DOWN + UP + ENTER, "one", "two", "three").equals("two");

        // The cursor does not go out of the items.
        assert select(UP + UP + UP + ENTER, "one", "two", "three").equals("one");

        assert select(DOWN + DOWN + DOWN + DOWN + ENTER, "one", "two", "three").equals("three");
    }

    @Test
    void selectByNumber() {
        assert select("3" + ENTER, "one", "two", "three").equals("three");

        // The backspace removes the last character.
        assert select("13" + "\u007f" + ENTER, "one", "two", "three").equals("one");
    }

    @Test
    void selectByInvalidInput() {
        assert select("9" + ENTER + DOWN + ENTER, "one", "two", "three").equals("two");

        assert user.receive("Invalid input, please retry.");
    }

    /**
     * Select an item using the scripted terminal input.
     * 
     * @param script A sequence of the key strokes.
     * @param items A list of the selectable items.
     * @return A selected item.
     */
    private String select(String script, String... items) {
        return new InteractiveUserInterface(user.output, user.error, user.input, new FakeTerminal(script)).ask("question", List.of(items));
    }

    /**
     * The command line user interface which is always able to use the interactive selector.
     */
    private static class InteractiveUserInterface extends CommandLineUserInterface {

        /** The simulated terminal. */
        private final Terminal terminal;

        /**
         * @param output A standard output.
         * @param error A standard error.
         * @param input A standard input.
         * @param terminal A simulated terminal.
         */
        InteractiveUserInterface(PrintStream output, PrintStream error, InputStream input, Terminal terminal) {
            super(output, error, input);
            this.terminal = terminal;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean selectorAvailable() {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Terminal terminal() {
            return terminal;
        }
    }

    /**
     * The terminal which reads the scripted key strokes instead of the real console.
     */
    private static class FakeTerminal extends Terminal {

        /** The scripted bytes. */
        private final Deque<Integer> keys = new ArrayDeque();

        /**
         * @param script A sequence of the key strokes.
         */
        FakeTerminal(String script) {
            for (byte b : script.getBytes(StandardCharsets.UTF_8)) {
                keys.add(b & 0xFF);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isEnabled() {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int read() {
            return keys.isEmpty() ? -1 : keys.poll();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() {
            // do nothing
        }
    }
}
