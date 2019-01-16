/*
 * Copyright (C) 2018 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee;

import bee.api.Library;
import bee.api.Scope;
import bee.task.Compile;
import bee.task.FindMain;
import bee.util.JarArchiver;
import psychopath.File;
import psychopath.Folder;
import psychopath.Locator;

public class Jar extends bee.task.Jar {

    /**
     * {@inheritDoc}
     */
    @Override
    public void merge() {
        require(Compile.class).source();
        String main = require(FindMain.class).main();

        File output = project.locateJar();
        ui.talk("Build merged classes jar: ", output);

        Folder folder = Locator.folder();
        folder.add(project.getClasses(), o -> o.strip());
        for (Library library : project.getDependency(Scope.Runtime)) {
            folder.add(library.getLocalJar().asArchive(), o -> o.glob("!META-INF/**"));
        }
        folder.packTo(output.extension("zip"));

        JarArchiver archiver = new JarArchiver();
        archiver.setMainClass(main);
        archiver.add(project.getClasses());
        // project.getSourceSet().to(dir -> {
        // archiver.add(dir.asJavaPath(), "**");
        // });

        for (Library library : project.getDependency(Scope.Runtime)) {
            archiver.add(library.getLocalJar().asJavaPath() /* , "!META-INF/**" */);
        }
        archiver.pack(output.asJavaPath());
    }
}
