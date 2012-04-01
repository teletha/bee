/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task.exe;

import java.nio.file.Path;

import kiss.I;
import bee.task.Command;
import bee.task.Task;
import bee.util.JarArchiver;

/**
 * @version 2012/04/01 9:34:01
 */
public class Exe extends Task {

    @Command(defaults = true)
    public void build() {
        Path jar = I.locate("test.jar");

        JarArchiver archiver = new JarArchiver();
        archiver.set("Manifest-Version", "1.0");
        archiver.set("Main-Class", Starter.class.getName());
        archiver.add(I.locate("target/classes"), "**/Starter.class");
        archiver.pack(jar);
    }
}
