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
import java.nio.file.StandardOpenOption;
import java.util.Objects;

import bee.UserInterface;
import bee.api.Project;
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

    /**
     * Write lines of text to a file. Each line is a char sequence and is written to the file in
     * sequence with each line terminated by the platform's line separator, as defined by the system
     * property {@code
     * line.separator}. Characters are encoded into bytes using the specified charset.
     * <p>
     * The {@code options} parameter specifies how the the file is created or opened. If no options
     * are present then this method works as if the {@link StandardOpenOption#CREATE CREATE},
     * {@link StandardOpenOption#TRUNCATE_EXISTING TRUNCATE_EXISTING}, and
     * {@link StandardOpenOption#WRITE WRITE} options are present. In other words, it opens the file
     * for writing, creating the file if it doesn't exist, or initially truncating an existing
     * {@link #isRegularFile regular-file} to a size of {@code 0}. The method ensures that the file
     * is closed when all lines have been written (or an I/O error or other runtime exception is
     * thrown). If an I/O error occurs then it may do so after the file has created or truncated, or
     * after some bytes have been written to the file.
     *
     * @param path the path to the file
     * @param lines an object to iterate over the char sequences
     * @param encoding the charset to use for encoding
     * @return the path
     * @throws UnsupportedOperationException if an unsupported option is specified
     * @throws SecurityException In the case of the default provider, and a security manager is
     *             installed, the {@link SecurityManager#checkWrite(String) checkWrite} method is
     *             invoked to check write access to the file.
     */
    public static void write(Path path, Iterable<? extends CharSequence> lines) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(lines);

        Project project = I.make(Project.class);

        try {
            Path directory = path.getParent();

            if (Files.notExists(directory)) {
                Files.createDirectories(directory);
            }
            Files.write(path, lines, project.getEncoding());
        } catch (IOException e) {
            I.make(UserInterface.class).error("Fail to write [", project.getRoot().relativize(path), "]");
            throw I.quiet(e);
        }
    }
}
