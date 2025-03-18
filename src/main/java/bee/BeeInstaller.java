/*
 * Copyright (C) 2024 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee;

import static bee.Platform.*;

import java.time.format.DateTimeFormatter;

import bee.api.Project;
import bee.api.Repository;
import bee.task.Help;
import kiss.I;
import psychopath.File;
import psychopath.Location;
import psychopath.Locator;

public class BeeInstaller {

    /** The date formatter. */
    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Launch Bee.
     */
    public static final void main(String... args) {
        I.load(Bee.class);
        install(true, true, true);
    }

    /**
     * Install Bee into your system.
     */
    public static final void install(boolean installLauncher, boolean installAPI, boolean showWelcome) {
        UserInterface ui = I.make(UserInterface.class);
        Project project = I.make(Project.class);

        Location location = Locator.locate(bee.Bee.class);
        if (location.isDirectory()) {
            // If Bee binary exists in the directory, it is likely to be executed from the
            // development environment of Bee itself.
            if (bee.Bee.Tool.equals(project)) {
                location = project.locateJar();
            } else {
                return;
            }
        }

        File source = location.asFile();
        if (installLauncher) {
            File dest = BeeHome.file("bee-" + bee.Bee.Tool.getVersion() + "-" + DATETIME.format(source.lastModifiedDateTime()) + ".jar");

            // The current bee.jar is newer.
            // We should copy it to JDK directory.
            // This process is mainly used by Bee users while install phase.
            if (source.lastModifiedMilli() != dest.lastModifiedMilli()) {
                // delete old jars
                BeeHome.walkFile("bee-*.jar").to(jar -> {
                    try {
                        // delete only bee-version-yyyyMMddhhmmss.jar
                        if (jar.base().length() > 18) {
                            jar.delete();
                        }
                    } catch (Exception e) {
                        // we can't delete current processing jar file.
                    }
                });

                // build jar
                source.copyTo(dest);
                ui.info("Build executor [", dest, "]");

                // build launcher
                Bee.text(String.format(Platform.isWindows() ? """
                        @echo off
                        %s -javaagent:"%s" -cp "%s" bee.Bee %%*
                        """ : """
                        #!/bin/bash
                        %s -javaagent:"%s" -cp "%s" bee.Bee "$@"
                        """, JavaHome.file("bin/java"), dest, dest));

                ui.info("Build launcher [", Bee, "]");
            }
        }

        if (installAPI) {
            File lib = bee.Bee.API.asLibrary().getLocalJar();
            if (lib.lastModifiedMilli() < source.lastModifiedMilli()) {
                File api = Locator.folder()
                        .add(source.asArchive(), "bee/**", "!**.java", "META-INF/services/javax.annotation.processing.Processor")
                        .packToTemporary();

                I.make(Repository.class).install(bee.Bee.API, api);
            }
        }

        if (showWelcome) {
            Task help = I.make(Help.class);
            help.execute("help:welcome", ui);
        }
    }
}