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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import kiss.I;

/**
 * @version 2012/10/23 13:31:46
 */
public class Prototype extends Task {

    @Command("Build standard Java project skelton.")
    public void java() {
        Path sources = project.getInput();

        try {
            Files.createDirectories(sources.resolve("main/java"));
            Files.createDirectories(sources.resolve("main/resources"));
            Files.createDirectories(sources.resolve("test/java"));
            Files.createDirectories(sources.resolve("test/resources"));
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * 
     */
    public void select() {

    }
}
