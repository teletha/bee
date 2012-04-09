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

        String value = ui.ask("test");

        assert value.equals(expected);
    }

    @Test
    public void inputWithDefault() throws Exception {
        String expected = "expected value";

        user.willInput("");

        String value = ui.ask("test", expected);

        assert value.equals(expected);
    }

    @Test
    public void bean() throws Exception {
        user.willInput("Charlotte Dunois");
        user.willInput("16");

        Bean bean = ui.ask(Bean.class);

        assert bean.getName().equals("Charlotte Dunois");
        assert bean.getAge() == 16;
    }

    /**
     * @version 2011/09/22 17:25:56
     */
    protected static class Bean {

        @Question(message = "name")
        private String name;

        @Question(message = "age")
        private int age;

        /**
         * Get the name property of this {@link CommandLineUserInterfaceTest.Bean}.
         * 
         * @return The name property.
         */
        public String getName() {
            return name;
        }

        /**
         * Set the name property of this {@link CommandLineUserInterfaceTest.Bean}.
         * 
         * @param name The name value to set.
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Get the age property of this {@link CommandLineUserInterfaceTest.Bean}.
         * 
         * @return The age property.
         */
        public int getAge() {
            return age;
        }

        /**
         * Set the age property of this {@link CommandLineUserInterfaceTest.Bean}.
         * 
         * @param age The age value to set.
         */
        public void setAge(int age) {
            this.age = age;
        }
    }
}
