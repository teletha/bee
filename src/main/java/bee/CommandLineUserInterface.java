/*
 * Copyright (C) 2015 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee;

import java.util.ArrayDeque;
import java.util.Deque;

import bee.api.Command;

/**
 * @version 2012/04/19 12:52:27
 */
class CommandLineUserInterface extends UserInterface {

    /** The task state. */
    private boolean first = false;

    /** The command queue. */
    private Deque<String> commands = new ArrayDeque();

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
        System.out.print(message);
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
