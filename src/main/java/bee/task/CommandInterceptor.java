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

import java.util.HashMap;
import java.util.Map;

import kiss.Interceptor;
import kiss.Manageable;
import bee.UserInterface;
import bee.api.ProjectSpecific;
import bee.util.Inputs;

/**
 * @version 2012/04/10 16:18:04
 */
@Manageable(lifestyle = ProjectSpecific.class)
class CommandInterceptor extends Interceptor<Command> {

    /** The executed commands pool. */
    private final Map<String, Object> commands = new HashMap();

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
        String name = Inputs.hyphenize(that.getClass().getSuperclass().getSimpleName()) + ":" + this.name;

        Object result = commands.get(name);

        if (!commands.containsKey(name)) {
            ui.startCommand(name, annotation);
            result = super.invoke(params);
            ui.endCommand(name, annotation);

            commands.put(name, result);
        }
        return result;
    }
}
