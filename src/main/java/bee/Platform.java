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

import java.nio.charset.Charset;
import java.util.Map.Entry;

import kiss.I;
import psychopath.Directory;
import psychopath.File;
import psychopath.Locator;

/**
 * Define platform specific default configurations.
 */
public final class Platform {

    /** The encoding. */
    public static final Charset Encoding = Charset.forName(System.getProperty("sun.jnu.encoding"));

    /** The line separator. */
    public static final String EOL = System.getProperty("line.separator");

    /** The executable file for Java. */
    public static final File Java;

    /** The root directory for Java. */
    public static final Directory JavaHome;

    /** The executable file for Bee. */
    public static final File Bee;

    /** The root directory for Bee. */
    public static final Directory BeeHome;

    /** The local repository. */
    public static final Directory BeeLocalRepository;

    /** The platform type. */
    private static boolean isWindows;

    /** The platform type. */
    private static boolean isLinux;

    /** The platform type. */
    private static boolean isGithub = System.getenv("GITHUB_ACTIONS") != null;

    /** The platform type. */
    private static boolean isJitPack = System.getenv("JITPACK") != null;

    /** The platform type. */
    private static boolean isEclipse;

    // initialization
    static {
        Directory bin = null;
        File javaExe = null;
        File beeExe = null;

        // Search Java SDK from path. Don't use java.home system property to avoid JRE.
        root: for (Entry<String, String> entry : System.getenv().entrySet()) {
            // On UNIX systems the alphabetic case of name is typically significant, while on
            // Microsoft Windows systems it is typically not.
            if (entry.getKey().equalsIgnoreCase("path")) {
                // Search classpath for Bee.
                for (String value : entry.getValue().split(java.io.File.pathSeparator)) {
                    if (value.toLowerCase().contains("plugins/org.eclipse.justj.openjdk")) {
                        isEclipse = true;
                        continue;
                    }

                    Directory directory = Locator.directory(value);
                    File linux = directory.file("javac");
                    File windows = directory.file("javac.exe");

                    if (linux.isPresent()) {
                        bin = directory;
                        javaExe = linux;
                        beeExe = directory.file("bee");
                        isLinux = true;

                        break root;
                    } else if (windows.isPresent()) {
                        bin = directory;
                        javaExe = windows;
                        beeExe = directory.file("bee.bat");
                        isWindows = true;

                        break root;
                    }
                }
            }
        }

        if (bin == null || javaExe == null) {
            throw new Error("Java SDK is not found in your environment path.");
        }

        Java = javaExe;
        JavaHome = javaExe.parent().parent();
        Bee = beeExe;
        BeeHome = JavaHome.directory("lib/bee");
        BeeLocalRepository = searchLocalRepository();
    }

    /**
     * Search maven home directory.
     * 
     * @return
     */
    private static Directory searchLocalRepository() {
        if (isJitPack) {
            return Locator.directory(System.getenv("HOME")).directory(".m2/repository");
        }

        for (Entry<String, String> entry : System.getenv().entrySet()) {
            if (entry.getKey().equalsIgnoreCase("path")) {
                for (String path : entry.getValue().split(java.io.File.pathSeparator)) {
                    File mvn = Locator.directory(path).file("mvn");
                    if (mvn.isPresent()) {
                        // maven is here
                        Directory home = mvn.parent().parent();
                        File conf = home.file("conf/settings.xml");

                        if (conf.isPresent()) {
                            String location = I.xml(conf.asJavaPath()).find("localRepository").text();
                            if (location.length() != 0) {
                                return Locator.directory(location);
                            }
                        }
                    }
                }
            }
        }
        return BeeHome.directory("repository");
    }

    /**
     * Hide constructor.
     */
    private Platform() {
    }

    /**
     * Check whether the current platform is Windows OS or not.
     * 
     * @return Result
     */
    public static boolean isWindows() {
        return isWindows;
    }

    /**
     * Check whether the current platform is Linux like OS or not.
     * 
     * @return Result
     */
    public static boolean isLinux() {
        return isLinux;
    }

    /**
     * Check whether the current platform is Github Action or not.
     * 
     * @return Result
     */
    public static boolean isGithub() {
        return isGithub;
    }

    /**
     * Check whether the current platform is JitPack or not.
     * 
     * @return Result
     */
    public static boolean isJitPack() {
        return isJitPack;
    }

    /**
     * Check whether the current platform is Eclipse or not.
     * 
     * @return Result
     */
    public static boolean isEclipse() {
        return isEclipse;
    }
}