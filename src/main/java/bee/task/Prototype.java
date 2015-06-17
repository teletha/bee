/*
 * Copyright (C) 2015 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import java.nio.file.Path;

import bee.api.Command;
import bee.api.Task;

/**
 * @version 2012/10/23 13:31:46
 */
public class Prototype extends Task {

    @Command("Build standard Java project skelton.")
    public void java() {
        String packageName = project.getGroup().replaceAll("\\.", "/");
        Path sources = project.getInput();

        makeDirectory(sources, "main/java/" + packageName);
        makeDirectory(sources, "main/resources/" + packageName);
        makeDirectory(sources, "test/java/" + packageName);
        makeDirectory(sources, "test/resources/" + packageName);
    }
}
