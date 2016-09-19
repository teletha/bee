/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import antibug.CommandLineUser;
import kiss.I;

/**
 * @version 2012/04/15 15:15:50
 */
public class CommandLineUserInterfaceTest {

    @Rule
    @ClassRule
    public static final CommandLineUser user = new CommandLineUser();

    private CommandLineUserInterface ui = new CommandLineUserInterface();

    @Test
    public void input() throws Exception {
        String expected = "expected value";

        user.willInput(expected);

        String answer = ui.ask("question");

        assert answer.equals(expected);
    }

    @Test
    public void inputEmpty() throws Exception {
        String expected = "expected value";

        user.willInput("");
        user.willInput(expected);

        String answer = ui.ask("question");

        assert answer.equals(expected);
    }

    @Test
    public void inputWithDefault() throws Exception {
        String expected = "expected value";

        user.willInput(expected);

        String answer = ui.ask("question", "woo hoo");

        assert answer.equals(expected);
    }

    @Test
    public void inputEmptyWithDefault() throws Exception {
        String expected = "expected value";

        user.willInput("");

        String answer = ui.ask("question", expected);

        assert answer.equals(expected);
    }

    @Test
    public void inputInt() throws Exception {
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
    @Ignore
    public void inputPath() throws Exception {
        Path def = I.locate("default");

        user.willInput("path");
        assert ui.ask("question", def).equals(I.locate("path"));

        user.willInput("path/with/directory");
        assert ui.ask("question", def).equals(I.locate("path/with/directory"));

        user.willInput("   ");
        assert ui.ask("question", def).equals(def);
    }

    @Test
    public void select() throws Exception {
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
