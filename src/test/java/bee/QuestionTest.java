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

import ezunit.CommandLineUser;

/**
 * @version 2011/09/23 15:05:36
 */
public class QuestionTest {

    @Rule
    public static final CommandLineUser user = new CommandLineUser();

    private CommandLineUserInterface ui = new CommandLineUserInterface();

    @Test
    public void properties() throws Exception {
        user.willInput("Charlotte Dunois");
        user.willInput("16");

        MultipleProperties bean = ui.ask(MultipleProperties.class);

        assert bean.getName().equals("Charlotte Dunois");
        assert bean.getAge() == 16;
    }

    /**
     * @version 2011/09/22 17:25:56
     */
    protected static class MultipleProperties {

        @Question(message = "name")
        private String name;

        @Question(message = "age")
        private int age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    @Test
    public void defaultValue() throws Exception {
        Default bean = ui.ask(Default.class);

        assert bean.getName().equals("default");
        assert bean.getAge() == 22;
    }

    /**
     * @version 2011/09/22 17:25:56
     */
    protected static class Default {

        @Question(message = "name")
        private String name = "default";

        @Question(message = "age")
        private int age = 22;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    @Test
    public void validate() throws Exception {
        user.willInput("Charlotte Dunois");
        user.willInput("22");
        user.willInput("16");

        Validation bean = ui.ask(Validation.class);

        assert bean.getName().equals("Charlotte Dunois");
        assert bean.getAge() == 16;
    }

    /**
     * @version 2011/09/22 17:25:56
     */
    protected static class Validation {

        @Question(message = "name")
        private String name;

        @Question(message = "age")
        private int age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            if (20 <= age) {
                throw new IllegalStateException("overextended");
            }
            this.age = age;
        }
    }
}
