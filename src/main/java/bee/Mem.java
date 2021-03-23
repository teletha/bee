/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee;

import bee.task.Help;
import bee.util.Inputs;

public class Mem extends Help {

    /**
     * {@inheritDoc}
     */
    @Override
    public void version() {
        String name = Inputs.hyphenize("className:methodName");

        Object result = Task.results.get(name);

        if (!Task.results.containsKey(name)) {
            System.out.println("OKOK");
            ui.startCommand(name, null);
            super.version();
            ui.endCommand(name, null);
            Task.results.put(name, result);
        }
    }
}
