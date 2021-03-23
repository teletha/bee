/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import bee.Task;
import bee.api.Command;
import psychopath.Directory;

public class Prototype extends Task {

    @Command("Generate standard Java project skelton.")
    public void java() {
        String packageName = project.getGroup().replaceAll("\\.", "/");
        Directory sources = project.getInput();

        makeDirectory(sources, "main/java/" + packageName);
        makeDirectory(sources, "main/resources/" + packageName);
        makeDirectory(sources, "test/java/" + packageName);
        makeDirectory(sources, "test/resources/" + packageName);
    }
}