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
import kiss.Singleton;
import bee.UserInterface;

/**
 * @version 2012/04/10 16:18:04
 */
@Manageable(lifestyle = Singleton.class)
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
        String name = that.getClass().getSuperclass().getSimpleName().toLowerCase() + ":" + this.name;

        Object result = commands.get(name);

        if (result == null) {
            ui.startCommand(name, annotation);
            result = super.invoke(params);
            ui.endCommand(name, annotation);

            if (result == null) {
                result = Void.TYPE;
            }
            commands.put(name, result);
        }

        // return original result (null in almost every commands)
        return result == Void.TYPE ? null : result;
    }
}
