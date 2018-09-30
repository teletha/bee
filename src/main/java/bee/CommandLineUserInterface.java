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

import java.util.ArrayDeque;
import java.util.Deque;

import bee.api.Command;

/**
 * @version 2018/09/30 12:49:55
 */
class CommandLineUserInterface extends UserInterface {

    /** The task state. */
    private boolean first = false;

    /** The command queue. */
    private Deque<String> commands = new ArrayDeque();

    /** The view state. */
    private boolean eraseNextLine = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public void startCommand(String name, Command command) {
        first = true;
        commands.add(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endCommand(String name, Command command) {
        if (first) {
            showCommandName();
        }
        first = true;
        System.out.print(Platform.EOL);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void write(String message) {
        if (first) {
            showCommandName();
            first = false;
        }

        if (eraseNextLine) {
            message = "[2K" + message;
        }
        eraseNextLine = message.endsWith("\r");

        if (eraseNextLine) {
            System.out.print(message);
        } else {
            System.out.println(message);
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
        return System.out;
    }

    /**
     * <p>
     * Show command name.
     * </p>
     */
    private void showCommandName() {
        String command = commands.pollLast();

        if (command != null) {
            System.out.println("◆ " + command + " ◆");
        }
    }
}
