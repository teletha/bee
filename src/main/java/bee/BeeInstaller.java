/*
 * Copyright (C) 2021 Nameless Production Committee
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
import java.util.ArrayList;
import java.util.List;

import bee.api.Project;
import bee.api.Repository;
import kiss.I;
import psychopath.File;
import psychopath.Locator;

public class BeeInstaller {

    /** The date formatter. */
    private static final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Launch Bee.
     */
    public static final void main(String... args) {
        I.load(Bee.class);
        install(true, false, true);
    }

    /**
     * Install Bee into your system.
     */
    public static final void install(boolean installCommand, boolean installFar, boolean installAPI) {
        UserInterface ui = I.make(UserInterface.class);
        Project project = I.make(Project.class);
        File source = bee.Bee.Tool.equals(project) ? project.locateJar() : Locator.locate(bee.Bee.class).asFile();

        if (installCommand) {
            String fileName = "bee-" + bee.Bee.Tool.getVersion() + "-" + format.format(source.lastModifiedDateTime()) + ".jar";
            File dest = BeeHome.file(fileName);

            // delete old files
            BeeHome.walkFile("bee-*.jar").to(jar -> {
                try {
                    // delete only bee-yyyyMMddhhmmss.jar
                    if (jar.base().length() > 18) {
                        jar.delete();
                    }
                } catch (Exception e) {
                    // we can't delete current processing jar file.
                }
            });

            if (source.lastModifiedMilli() != dest.lastModifiedMilli()) {
                // The current bee.jar is newer.
                // We should copy it to JDK directory.
                // This process is mainly used by Bee users while install phase.
                source.copyTo(dest);
                ui.info("Write new bee library. [", dest, "]");
            }

            // create bat file
            List<String> bat = new ArrayList();

            if (Bee.name().endsWith(".bat")) {
                // windows use JDK full path to avoid using JRE
                bat.add("@echo off");
                bat.add(JavaHome.file("bin/java") + " -javaagent:\"" + dest + "\" -cp \"" + dest + "\" " + Bee.class.getName() + " %*");
            } else {
                // linux
                bat.add("#!/bin/bash");
                bat.add(JavaHome.file("bin/java") + " -javaagent:\"" + dest + "\"-cp \"" + dest + "\" " + Bee.class.getName() + " \"$@\"");
            }
            Bee.text(bat);

            ui.info("Write new bat file. [", Bee, "]");
        }

        if (installAPI) {
            File api = Locator.folder()
                    .add(source.asArchive(), "bee/**", "!**.java")
                    .add(source.asArchive(), "META-INF/services/**")
                    .packToTemporary();

            I.make(Repository.class).install(bee.Bee.API, api);
        }
    }
}