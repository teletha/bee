/*
 * Copyright (C) 2026 The BEE Development Team
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
import psychopath.Directory;
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
        File source = source();
        File dest = executorJar(source);

        if (installLauncher) {
            // The current bee.jar is newer.
            // We should copy it to the Bee home directory.
            // This process is mainly used by Bee users while install phase.
            if (source.lastModifiedMilli() != dest.lastModifiedMilli()) {
                // delete old jars
                Platform.BeeHome.walkFile("bee-*.jar", "bee-*.aot", "bee-*.aotconf").to(jar -> {
                    try {
                        // delete only bee-version-yyyyMMddhhmmss.jar
                        if (jar.base().length() > 18) {
                            jar.deleteOnExit();
                        }
                    } catch (Exception e) {
                        // we can't delete current processing jar file.
                    }
                });

                // build jar
                source.copyTo(dest);
                ui.info("Install bee executor to ", dest);

                // build launcher
                writeLauncher(dest, Platform.JavaHome);
                ui.info("Install bee launcher to ", Platform.Bee);
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

    /**
     * Resolve the jar which provides the executor to install. While Bee builds itself, the freshly
     * built jar is the source, otherwise the running jar is.
     * 
     * @return A source jar.
     */
    private static File source() {
        Project project = I.make(Project.class);
        return Bee.Tool.equals(project) ? project.locateJar() : Locator.locate(Bee.class).asFile();
    }

    /**
     * Resolve the executor jar which the launcher starts.
     * 
     * @return The installed executor jar.
     */
    public static File executorJar() {
        return executorJar(source());
    }

    /**
     * Resolve the installed executor jar path for the specified source jar.
     * 
     * @param source A source jar.
     * @return The installed executor jar.
     */
    private static File executorJar(File source) {
        return Platform.BeeHome
                .file("bee-" + Bee.Tool.getVersion() + "-" + DATETIME.format(source.lastModifiedDateTime()) + ".jar");
    }

    /**
     * Write the launcher which starts the specified executor jar with the specified JDK. The JDK is
     * resolved to its real directory and hardcoded, so the launcher never depends on the mutable
     * "selected" link.
     * 
     * @param jar The executor jar.
     * @param javaHome The JDK used by the launcher.
     */
    public static void writeLauncher(File jar, Directory javaHome) {
        Directory home;
        try {
            home = Locator.directory(javaHome.asJavaPath().toRealPath());
        } catch (Exception e) {
            home = javaHome;
        }
        File java = home.file(Platform.isWindows() ? "bin/java.exe" : "bin/java");

        // The AOT cache is specific to the JDK which created it, so the file name contains the JDK
        // feature version. A missing cache is created automatically on startup, and the aot log is
        // disabled so that a missing cache does not print an error.
        int feature = Platform.feature(home);
        if (feature < 0) {
            feature = Runtime.version().feature();
        }
        String aot = jar + "." + feature + ".aot";

        // On Windows the java call is followed by "& call :exitWithErrorLevel" on the same line. The
        // trailing call moves cmd.exe past the interruption which Ctrl+C sets, so it does not ask
        // "Terminate batch job (Y/N)?", and the label exits with the real code. This is the same
        // pattern which the Gradle wrapper uses.
        Platform.Bee.text(String.format(Platform.isWindows()
                ? """
                        @echo off
                        %s -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -XX:AOTCache=%s -Xlog:aot*=off -XX:+IgnoreUnrecognizedVMOptions --enable-native-access=ALL-UNNAMED -cp "%s" bee.Bee %%* & call :exitWithErrorLevel

                        :exitWithErrorLevel
                        @rem Use "%%COMSPEC%%" /c exit to allow operators to work properly in scripts
                        "%%COMSPEC%%" /c exit %%ERRORLEVEL%%
                        """
                : """
                        #!/bin/bash
                        %s -XX:+TieredCompilation -XX:TieredStopAtLevel=1  -XX:AOTCache=%s -Xlog:aot*=off -XX:+IgnoreUnrecognizedVMOptions --enable-native-access=ALL-UNNAMED -cp "%s" bee.Bee "$@"
                        """, java, aot, jar));
    }
}