/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import kiss.Interceptor;
import bee.UserInterface;

/**
 * @version 2012/04/10 16:18:04
 */
class CommandInterceptor extends Interceptor<Command> {

    /** The user interface. */
    private final UserInterface ui;

    /**
     * @param ui
     */
    private CommandInterceptor(UserInterface ui) {
        this.ui = ui;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object invoke(Object... params) {
        String name = that.getClass().getSuperclass().getSimpleName().toLowerCase() + ":" + this.name;
        String description = annotation.description();

        ui.talk(">>> ", name, " # ", description, " >>>");
        Object result = super.invoke(params);
        // ui.talk("<<< ", name, " <<<");

        return result;
    }
}
