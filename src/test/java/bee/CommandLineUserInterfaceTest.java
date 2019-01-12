/*
 * Copyright (C) 2018 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import antibug.CommandLineUser;

class CommandLineUserInterfaceTest {

    @RegisterExtension
    static final CommandLineUser user = new CommandLineUser();

    private CommandLineUserInterface ui = new CommandLineUserInterface();

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
}
