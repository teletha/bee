/*
 * Copyright (C) 2026 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task.sample;

import bee.Task;
import bee.api.Command;

public interface SameNameExternalPackage extends Task {
    @Command("")
    default String command() {
        return "same name in external package";
    }
}