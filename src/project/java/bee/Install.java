/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee;

import bee.api.ArtifactLocator;
import bee.task.Command;
import bee.util.ZipArchiver;

/**
 * @version 2012/05/10 10:29:33
 */
public class Install extends bee.task.Install {

    /**
     * <p>
     * Install the current Bee into your environment.
     * </p>
     */
    @Override
    @Command("Install the current Bee into your environment.")
    public void project() {
        require(Jar.class).merge();

        BeeInstaller.install(ArtifactLocator.Jar.in(project));
        ZipArchiver archiver = new ZipArchiver();
        archiver.add(ArtifactLocator.Jar.in(project));
        archiver.unpack(project.getOutput().resolve("test"));
    }
}
