/*
 * Copyright (C) 2024 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import static bee.TaskOperations.*;

import bee.Task;
import bee.api.Command;

public interface Maven extends Task {

    @Command("Generate pom file.")
    default void pom() {
        makeFile("pom.xml", project().toMavenDefinition());
    }
}