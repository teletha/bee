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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import antibug.CommandLineUser;
import bee.UserInterface.CommandLineUserInterface;

class CommandLineUserInterfaceTest {

    /** The escape sequence which terminals send when the user presses the down arrow key. */
    private static final String DOWN = "\u001b[B";

    /** The escape sequence which terminals send when the user presses the up arrow key. */
    private static final String UP = "\u001b[A";

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
    void select() {
        List<String> items = new ArrayList();
        items.add("one");
        items.add("two");
        items.add("three");

        user.willInput("1");
        assert ui.ask("question", items).equals("one");

        user.willInput("2");
        assert ui.ask("question", items).equals("two");

        user.willInput("3");
        assert ui.ask("question", items).equals("three");
    }

    @Test
    void selectByEnterKey() {
        UserInterface ui = tui();

        user.willInput("");

        assert ui.ask("question", List.of("one", "two", "three")).equals("one");
    }

    @Test
    void selectByArrowKey() {
        UserInterface ui = tui();

        user.willInput(DOWN, "");
        assert ui.ask("question", List.of("one", "two", "three")).equals("two");

        user.willInput(DOWN, DOWN, UP, "");
        assert ui.ask("question", List.of("one", "two", "three")).equals("two");

        // The cursor does not go out of the items.
        user.willInput(UP, UP, UP, "");
        assert ui.ask("question", List.of("one", "two", "three")).equals("one");

        user.willInput(DOWN, DOWN, DOWN, DOWN, "");
        assert ui.ask("question", List.of("one", "two", "three")).equals("three");
    }

    @Test
    void selectByNumber() {
        UserInterface ui = tui();

        user.willInput("3");
        assert ui.ask("question", List.of("one", "two", "three")).equals("three");
    }

    @Test
    void selectByInvalidInput() {
        UserInterface ui = tui();

        user.willInput("woo hoo", "9", DOWN, "");
        assert ui.ask("question", List.of("one", "two", "three")).equals("two");

        assert user.receive("Invalid input, please retry.");
    }

    /**
     * Build an interactive user interface. The test environment has no terminal, so we have to
     * enable the interactive selector explicitly.
     *
     * @return An interactive user interface.
     */
    private UserInterface tui() {
        return new InteractiveUserInterface(user.output, user.error, user.input);
    }

    /**
     * The command line user interface which is always able to use the interactive selector.
     */
    private static class InteractiveUserInterface extends CommandLineUserInterface {

        /**
         * @param output A standard output.
         * @param error A standard error.
         * @param input A standard input.
         */
        InteractiveUserInterface(PrintStream output, PrintStream error, InputStream input) {
            super(output, error, input);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean selectorAvailable() {
            return true;
        }
    }
}