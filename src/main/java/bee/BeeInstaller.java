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

import static bee.Platform.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import bee.api.Repository;
import bee.util.JarArchiver;
import bee.util.Paths;
import kiss.I;
import kiss.model.ClassUtil;

/**
 * @version 2015/06/22 11:53:31
 */
public class BeeInstaller {

    /** The date formatter. */
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");

    /**
     * <p>
     * Launch Bee.
     * </p>
     */
    public static final void main(String... args) {
        install(ClassUtil.getArchive(BeeInstaller.class));
    }

    /**
     * <p>
     * Install Bee into your system.
     * </p>
     * 
     * @param source
     */
    public static final void install(Path source) {
        UserInterface ui = I.make(UserInterface.class);

        try {
            String fileName = "bee-" + format.format(new Date(Files.getLastModifiedTime(source).toMillis())) + ".jar";
            Path dest = BeeHome.resolve(fileName);

            // delete old files
            for (Path jar : I.walk(BeeHome, "bee-*.jar")) {
                try {
                    Files.delete(jar);
                } catch (Exception e) {
                    // we can't delete current processing jar file.
                }
            }

            if (Paths.getLastModified(source) != Paths.getLastModified(dest)) {
                // The current bee.jar is newer.
                // We should copy it to JDK directory.
                // This process is mainly used by Bee users while install phase.
                I.copy(source, dest);
                ui.talk("Install new bee library. [", dest, "]");
            }

            // create bat file
            List<String> bat = new ArrayList();

            if (Bee.getFileName().toString().endsWith(".bat")) {
                // windows
                // use JDK full path to avoid using JRE
                bat.add("@echo off");
                bat.add(JavaHome.resolve("bin/java") + " -cp \"" + dest.toString() + "\" " + Bee.class
                        .getName() + " %*");
            } else {
                // linux
                // TODO
            }
            Files.write(Bee, bat, I.$encoding);

            ui.talk("Write new bat file. [", Bee, "]");

            // create bee-api library and sources
            Path classes = I.locateTemporary();
            JarArchiver archiver = new JarArchiver();
            archiver.add(source, "bee/**", "!**.java");
            archiver.add(source, "META-INF/services/**");
            archiver.pack(classes);

            Path sources = I.locateTemporary();
            archiver = new JarArchiver();
            archiver.add(source, "bee/**.java");
            archiver.add(source, "META-INF/services/**");
            archiver.pack(sources);

            I.make(Repository.class).install(bee.Bee.API, classes, sources);
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }
}
