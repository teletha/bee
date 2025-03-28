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

import java.time.format.DateTimeFormatter;

import bee.api.Project;
import bee.api.Repository;
import kiss.I;
import psychopath.File;
import psychopath.Locator;

/**
 * Handles the installation process for the Bee build tool.
 * This includes installing the Bee executable JAR, creating launcher scripts,
 * installing the Bee API into the local repository, and displaying a welcome message.
 */
public class BeeInstaller {

    /** The date formatter for timestamped JAR file names (yyyyMMddHHmmss). */
    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * The main entry point for launching the Bee installation process.
     * Loads the Bee core and then calls {@link #install(boolean, boolean, boolean)}
     * with default options (install launcher, install API, show welcome message).
     *
     * @param args Command line arguments (not used).
     */
    public static final void main(String... args) {
        I.load(Bee.class);
        install(true, true, true);
    }

    /**
     * Installs Bee components into the user's system based on the provided flags.
     *
     * @param installLauncher If {@code true}, installs the Bee executable JAR (with version and
     *            timestamp) into the Bee home directory ({@link Platform#BeeHome}) and creates or
     *            updates the native launcher script ({@code bee} or {@code bee.bat}). Older
     *            timestamped JARs are removed. Installation only proceeds if the source JAR is
     *            newer than the potentially existing destination JAR.
     * @param installAPI If {@code true}, installs the Bee API library (extracted from the source
     *            JAR) into the local Maven repository. Installation only proceeds if the API JAR in
     *            the repository is older than the source JAR.
     * @param showWelcome If {@code true}, executes the {@code bee help:welcome} command to display
     *            a welcome message to the user after installation steps.
     */
    public static final void install(boolean installLauncher, boolean installAPI, boolean showWelcome) {
        UserInterface ui = I.make(UserInterface.class);
        Project project = I.make(Project.class);

        File source = Bee.Tool.equals(project) ? project.locateJar() : Locator.locate(Bee.class).asFile();

        if (installLauncher) {
            File dest = Platform.BeeHome
                    .file("bee-" + Bee.Tool.getVersion() + "-" + DATETIME.format(source.lastModifiedDateTime()) + ".jar");
            // The current bee.jar is newer.
            // We should copy it to JDK directory.
            // This process is mainly used by Bee users while install phase.
            if (source.lastModifiedMilli() != dest.lastModifiedMilli()) {
                // delete old jars
                Platform.BeeHome.walkFile("bee-*.jar", "bee-*.aot", "bee-*.aotconf").to(jar -> {
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
                String optionAOT = Runtime.version().feature() < 24 ? "" : "-XX:AOTCache=" + dest + ".aot";

                Platform.Bee.text(String.format(Platform.isWindows() ? """
                        @echo off
                        %s -XX:+TieredCompilation -XX:TieredStopAtLevel=1 %s -cp "%s" bee.Bee %%*
                        """ : """
                        #!/bin/bash
                        %s -XX:+TieredCompilation -XX:TieredStopAtLevel=1 %s -cp "%s" bee.Bee "$@"
                        """, Platform.JavaHome.file("bin/java"), optionAOT, dest));

                ui.info("Build launcher [", Platform.Bee, "]");
            }
        }

        if (installAPI) {
            File lib = bee.Bee.API.asLibrary().getLocalJar();
            if (lib.lastModifiedMilli() < source.lastModifiedMilli()) {
                File api = Locator.folder()
                        .add(source.asArchive(), "bee/**", "!**.java", "META-INF/services/javax.annotation.processing.Processor")
                        .packToTemporary();

                I.make(Repository.class).install(Bee.API, api);
            }
        }

        if (showWelcome) {
            Bee.execute("help:welcome");
        }
    }
}