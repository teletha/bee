/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.maven;

import static kiss.Element.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map.Entry;

import kiss.I;

/**
 * @version 2012/03/21 16:39:00
 */
public class Maven {

    /** The maven home directory. */
    public static final Path Home = searchMavenHome();

    /** The maven repository directory. */
    public static final Path Repository = searchLocalRepository();

    /**
     * <p>
     * Search maven home directory.
     * </p>
     * 
     * @return
     */
    private static Path searchMavenHome() {
        for (Entry<String, String> entry : System.getenv().entrySet()) {
            if (entry.getKey().equalsIgnoreCase("path")) {
                for (String path : entry.getValue().split(File.pathSeparator)) {
                    Path mvn = I.locate(path).resolve("mvn");

                    if (Files.exists(mvn)) {
                        return mvn.getParent().getParent();
                    }
                }
            }
        }
        // If this exception will be thrown, it is bug of this program. So we must rethrow the
        // wrapped error in here.
        throw new Error();
    }

    /**
     * <p>
     * Search maven local respository.
     * </p>
     * 
     * @return
     */
    private static Path searchLocalRepository() {
        Path conf = Home.resolve("conf/settings.xml");

        if (Files.exists(conf)) {
            String path = $(conf).find("localRepository").text();

            if (path.length() != 0) {
                return I.locate(path);
            }
        }
        return null;
    }

    public static void main(String[] args) {
        System.out.println(Home);
        System.out.println(Repository);
    }
}
