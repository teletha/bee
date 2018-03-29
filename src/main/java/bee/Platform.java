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

/**
 * <p>
 * Define platform specific default configurations.
 * </p>
 * 
 * @version 2016/12/10 18:19:44
 */
public final class Platform {

    /** The encoding. */
    public static final Charset Encoding = Charset.forName(System.getProperty("sun.jnu.encoding"));

    /** The line separator. */
    public static final String EOL = System.getProperty("line.separator");

    /** The executable file for Java. */
    public static final Path Java;

    /** The root directory for Java. */
    public static final Path JavaHome;

    /** The rt.jar file for Java. */
    public static final Path JavaRuntime;

    /** The executable file for Bee. */
    public static final Path Bee;

    /** The root directory for Bee. */
    public static final Path BeeHome;

    /** The local repository. */
    public static final Path BeeLocalRepository;

    /** The platform type. */
    private static boolean isWindows;

    /** The platform type. */
    private static boolean isLinux;

    // initialization
    static {
        Path bin = null;
        Path java = null;
        Path bee = null;

        // Search Java SDK from path. Don't use java.home system property to avoid JRE.
        root: for (Entry<String, String> entry : System.getenv().entrySet()) {
            // On UNIX systems the alphabetic case of name is typically significant, while on
            // Microsoft Windows systems it is typically not.
            if (entry.getKey().equalsIgnoreCase("path")) {
                // Search classpath for Bee.
                for (String value : entry.getValue().split(File.pathSeparator)) {
                    Path directory = Filer.locate(value);
                    Path linux = directory.resolve("javac");
                    Path windows = directory.resolve("javac.exe");

                    if (Files.exists(linux)) {
                        bin = directory;
                        java = linux;
                        bee = directory.resolve("bee");
                        isLinux = true;

                        break root;
                    } else if (Files.exists(windows)) {
                        bin = directory;
                        java = windows;
                        bee = directory.resolve("bee.bat");
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
        JavaHome = java.getParent().getParent();
        JavaRuntime = JavaHome.resolve("jre/lib/rt.jar");
        Bee = bee;
        BeeHome = JavaHome.resolve("lib/bee");
        BeeLocalRepository = searchLocalRepository();
    }

    /**
     * <p>
     * Search maven home directory.
     * </p>
     * 
     * @return
     */
    private static Path searchLocalRepository() {
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
                                return Filer.locate(location);
                            }
                        }
                    }
                }
            }
        }
        return BeeHome.resolve("repository");
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
