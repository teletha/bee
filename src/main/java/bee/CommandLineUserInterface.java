/*
 * Copyright (C) 2010 Nameless Production Committee.
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

import java.util.ArrayDeque;
import java.util.Deque;

import bee.task.Command;

/**
 * @version 2010/11/23 23:24:52
 */
class CommandLineUserInterface extends UserInterface {

    private boolean commandStarted = false;

    private Deque<String> commands = new ArrayDeque();

    /**
     * {@inheritDoc}
     */
    @Override
    public void startCommand(String name, Command command) {
        commandStarted = true;
        commands.add(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endCommand(String name, Command command) {
        commandStarted = true;

        if (name.equals(commands.peekLast())) {
            commands.pollLast();

            System.out.println("◆ " + name + " ◆");
        }
        System.out.print(Platform.EOL);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void write(String message) {
        if (commandStarted) {
            commandStarted = false;

            String command = commands.pollLast();

            if (command != null) {
                System.out.println("◆ " + command + " ◆");
            }
        }
        System.out.print(message);
    }
}
