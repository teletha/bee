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

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map.Entry;

import filer.Filer;
import kiss.I;
import psychopath.Directory;
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
    public static final psychopath.File Java;

    /** The root directory for Java. */
    public static final Directory JavaHome;

    /** The executable file for Bee. */
    public static final psychopath.File Bee;

    /** The root directory for Bee. */
    public static final Directory BeeHome;

    /** The local repository. */
    public static final Directory BeeLocalRepository;

    /** The platform type. */
    private static boolean isWindows;

    /** The platform type. */
    private static boolean isLinux;

    // initialization
    static {
        Directory bin = null;
        psychopath.File java = null;
        psychopath.File bee = null;

        // Search Java SDK from path. Don't use java.home system property to avoid JRE.
        root: for (Entry<String, String> entry : System.getenv().entrySet()) {
            // On UNIX systems the alphabetic case of name is typically significant, while on
            // Microsoft Windows systems it is typically not.
            if (entry.getKey().equalsIgnoreCase("path")) {
                // Search classpath for Bee.
                for (String value : entry.getValue().split(File.pathSeparator)) {
                    Directory directory = Locator.directory(value);
                    psychopath.File linux = directory.file("javac");
                    psychopath.File windows = directory.file("javac.exe");

                    if (linux.isPresent()) {
                        bin = directory;
                        java = linux;
                        bee = directory.file("bee");
                        isLinux = true;

                        break root;
                    } else if (windows.isPresent()) {
                        bin = directory;
                        java = windows;
                        bee = directory.file("bee.bat");
                        isWindows = true;

                        break root;
                    }
                }
            }
        }

        if (bin == null || java == null) {
            throw new Error("Java SDK is not found in your environment path.");
        }

        Java = java;
        JavaHome = java.parent().parent();
        Bee = bee;
        BeeHome = JavaHome.directory("lib/bee");
        BeeLocalRepository = searchLocalRepository();
    }

    /**
     * <p>
     * Search maven home directory.
     * </p>
     * 
     * @return
     */
    private static Directory searchLocalRepository() {
        for (Entry<String, String> entry : System.getenv().entrySet()) {
            if (entry.getKey().equalsIgnoreCase("path")) {
                for (String path : entry.getValue().split(File.pathSeparator)) {
                    Path mvn = Filer.locate(path).resolve("mvn");

                    if (Files.exists(mvn)) {
                        // maven is here
                        Path home = mvn.getParent().getParent();
                        Path conf = home.resolve("conf/settings.xml");

                        if (Files.exists(conf)) {
                            String location = I.xml(conf.toFile()).find("localRepository").text();

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
     * <p>
     * Check platform.
     * </p>
     * 
     * @return
     */
    public static boolean isWindows() {
        return isWindows;
    }

    /**
     * <p>
     * Check platform.
     * </p>
     * 
     * @return
     */
    public static boolean isLinux() {
        return isLinux;
    }
}
