/*
 * Copyright (C) 2017 Nameless Production Committee
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
 * @version 2017/03/08 1:46:27
 */
public class Pom extends Task {

    @Command("Generate pom file.")
    public void gitignore() {
        Path pom = project.getRoot().resolve("pom.xml");

        makeFile(pom, project.toString());
    }
}
