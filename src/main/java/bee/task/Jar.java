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

import java.nio.file.Path;

import kiss.I;
import bee.util.JarArchiver;

/**
 * @version 2012/04/01 23:18:15
 */
public class Jar extends Task {

    /** The compile task. */
    private final Compile compile = I.make(Compile.class);

    @Command(defaults = true, description = "Package main classes and other resources.")
    public void source() {
        compile.source();

        Path jar = project.getOutput().resolve(project.getProduct() + "-" + project.getVersion() + ".jar");

        ui.talk("Build main classes jar: ", jar);
        JarArchiver archiver = new JarArchiver();
        archiver.add(project.getClasses());
        archiver.pack(jar);
    }

    private void pack(Path input, Path output) {

    }
}
