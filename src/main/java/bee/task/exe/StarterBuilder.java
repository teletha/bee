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

import java.io.IOException;
import java.nio.file.Path;

import kiss.I;
import bee.task.JarArchiver;

/**
 * @version 2012/03/31 23:02:42
 */
public class StarterBuilder {

    public static void main(String[] args) {
        Path jar = I.locate("test.jar");

        JarArchiver archiver = new JarArchiver();
        archiver.set("Manifest-Version", "1.0");
        archiver.set("Main-Class", Starter.class.getName());
        archiver.add(I.locate("target/classes"), "**/Starter.class");
        archiver.pack(jar);

        try {
            Runtime.getRuntime().exec("exewrap test.jar -o f:\\application\\Toybox\\Toybox2.exe -g");
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }
}
