import java.nio.file.Path;

import bee.BeeInstaller;
import bee.definition.ArtifactLocator;
import bee.definition.Library;
import bee.definition.Scope;
import bee.task.Compile;
import bee.util.JarArchiver;

/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */

/**
 * @version 2012/04/12 0:08:30
 */
public class Jar extends bee.task.Jar {

    /**
     * {@inheritDoc}
     */
    @Override
    public void source() {
        require(Compile.class).source();

        Path output = ArtifactLocator.Jar.in(project);
        ui.talk("Build bee jar: ", output);

        JarArchiver archiver = new JarArchiver();
        archiver.setMainClass(BeeInstaller.class);
        archiver.add(project.getClasses());
        for (Library library : project.getDependency("npc", "sinobu", Scope.Runtime)) {
            archiver.add(library.getJar());
        }

        archiver.pack(output);
    }
}
