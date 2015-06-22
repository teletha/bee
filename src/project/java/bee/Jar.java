/*
 * Copyright (C) 2015 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee;

import java.nio.file.Path;

import bee.api.Library;
import bee.api.Scope;
import bee.task.Compile;
import bee.task.FindMain;
import bee.util.JarArchiver;

/**
 * @version 2012/04/18 10:46:11
 */
public class Jar extends bee.task.Jar {

    /**
     * {@inheritDoc}
     */
    @Override
    public void merge() {
        require(Compile.class).source();
        String main = require(FindMain.class).main();

        Path output = project.locateJar();
        ui.talk("Build merged classes jar: ", output);

        JarArchiver archiver = new JarArchiver();
        archiver.setMainClass(main);
        archiver.add(project.getClasses().base);
        archiver.add(project.getSources());

        for (Library library : project.getDependency(Scope.Runtime)) {
            archiver.add(library.getJar());
        }
        archiver.pack(output);
    }
}
