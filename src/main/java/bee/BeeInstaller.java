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

import bee.api.Repository;
import kiss.I;
import psychopath.File;
import psychopath.Locator;

public class BeeInstaller {

    /** The date formatter. */
    private static final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * <p>
     * Launch Bee.
     * </p>
     */
    public static final void main(String... args) {
        install(Locator.locate(BeeInstaller.class).asFile());
    }

    /**
     * <p>
     * Install Bee into your system.
     * </p>
     * 
     * @param source
     */
    public static final void install(File source) {
        UserInterface ui = I.make(UserInterface.class);

        String fileName = "bee-" + format.format(source.lastModifiedDateTime()) + ".jar";
        psychopath.File dest = BeeHome.file(fileName);

        // delete old files
        BeeHome.walkFile("bee-*.jar").to(jar -> {
            try {
                jar.delete();
            } catch (Exception e) {
                // we can't delete current processing jar file.
            }
        });

        if (source.lastModifiedMilli() != dest.lastModifiedMilli()) {
            // The current bee.jar is newer.
            // We should copy it to JDK directory.
            // This process is mainly used by Bee users while install phase.
            source.copyTo(dest);
            ui.talk("Install new bee library. [", dest, "]");
        }

        // create bat file
        List<String> bat = new ArrayList();

        if (Bee.name().endsWith(".bat")) {
            // windows use JDK full path to avoid using JRE
            bat.add("@echo off");
            bat.add(JavaHome.file("bin/java") + " --enable-preview -Xms256m -Xmx2048m -cp \"" + dest.toString() + "\" " + Bee.class
                    .getName() + " %*");
        } else {
            // linux
            // TODO
        }
        Bee.text(bat);

        ui.talk("Write new bat file. [", Bee, "]");

        // create bee-api library and sources
        File api = Locator.folder()
                .add(source.asArchive(), "bee/**", "!**.java")
                .add(source.asArchive(), "META-INF/services/**")
                .packToTemporary();

        I.make(Repository.class).install(bee.Bee.API, api);
    }
}