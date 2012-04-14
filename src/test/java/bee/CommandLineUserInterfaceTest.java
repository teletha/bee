/*
 * Copyright (C) 2011 Nameless Production Committee.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bee;

import java.nio.file.Path;

import kiss.I;

import org.junit.Rule;
import org.junit.Test;

import antibug.CommandLineUser;

/**
 * @version 2011/09/22 15:10:39
 */
public class CommandLineUserInterfaceTest {

    @Rule
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
    public void inputPath() throws Exception {
        Path def = I.locate("default");

        user.willInput("path");
        assert ui.ask("question", def).equals(I.locate("path"));

        user.willInput("path/with/directory");
        assert ui.ask("question", def).equals(I.locate("path/with/directory"));

        user.willInput("   ");
        assert ui.ask("question", def).equals(def);
    }
}
