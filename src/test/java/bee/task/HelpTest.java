/*
 * Copyright (C) 2025 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import org.junit.jupiter.api.Test;

import bee.Help;
import bee.InlineProjectAware;
import bee.Task;

class HelpTest extends InlineProjectAware {

    @Test
    void task() {
        Help help = Task.by(Help.class);
        help.task();
    }
}
