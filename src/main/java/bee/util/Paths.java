/*
 * Copyright (C) 2015 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import kiss.I;

/**
 * @version 2012/03/28 23:39:51
 */
public class Paths {

    /**
     * <p>
     * Compute file name.
     * </p>
     * 
     * @param path A target file.
     * @return A file name without extension.
     */
    public static String getName(Path path) {
        if (path == null) {
            return "";
        }

        String name = path.getFileName().toString();

        int index = name.lastIndexOf('.');

        if (index == -1) {
            return name;
        } else {
            return name.substring(0, index);
        }
    }

    /**
     * <p>
     * Compute file extension.
     * </p>
     * 
     * @param path A target file.
     * @return A file extension without name.
     */
    public static String getExtension(Path path) {
        if (path == null) {
            return "";
        }

        String name = path.getFileName().toString();

        int index = name.lastIndexOf('.');

        if (index == -1) {
            return "";
        } else {
            return name.substring(index + 1);
        }
    }

    /**
     * <p>
     * Compute last modified time of the specified path..
     * </p>
     * 
     * @param path A target file.
     * @return A last modified time.
     */
    public static long getLastModified(Path path) {
        if (path == null) {
            return 0;
        }

        if (Files.notExists(path)) {
            return -1;
        }

        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }
}
