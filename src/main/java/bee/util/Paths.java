/*
 * Copyright (C) 2018 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import bee.UserInterface;
import bee.api.Project;
import filer.Filer;
import kiss.I;

/**
 * @version 2017/01/10 15:32:12
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
            I.make(UserInterface.class).error("Fail to write [", path, "]");
            throw I.quiet(e);
        }
    }

    /**
     * Writes bytes to a file. The {@code options} parameter specifies how the the file is created
     * or opened. If no options are present then this method works as if the
     * {@link StandardOpenOption#CREATE CREATE}, {@link StandardOpenOption#TRUNCATE_EXISTING
     * TRUNCATE_EXISTING}, and {@link StandardOpenOption#WRITE WRITE} options are present. In other
     * words, it opens the file for writing, creating the file if it doesn't exist, or initially
     * truncating an existing {@link #isRegularFile regular-file} to a size of {@code 0}. All bytes
     * in the byte array are written to the file. The method ensures that the file is closed when
     * all bytes have been written (or an I/O error or other runtime exception is thrown). If an I/O
     * error occurs then it may do so after the file has created or truncated, or after some bytes
     * have been written to the file.
     * <p>
     * <b>Usage example</b>: By default the method creates a new file or overwrites an existing
     * file. Suppose you instead want to append bytes to an existing file: <pre>
     *     Path path = ...
     *     byte[] bytes = ...
     *     Files.write(path, bytes, StandardOpenOption.APPEND);
     * </pre>
     *
     * @param path the path to the file
     * @param bytes the byte array with the bytes to write
     * @param options options specifying how the file is opened
     * @return the path
     * @throws IOException if an I/O error occurs writing to or creating the file
     * @throws UnsupportedOperationException if an unsupported option is specified
     * @throws SecurityException In the case of the default provider, and a security manager is
     *             installed, the {@link SecurityManager#checkWrite(String) checkWrite} method is
     *             invoked to check write access to the file.
     */
    public static Path write(String text) {
        Objects.requireNonNull(text);

        Path file = Filer.locateTemporary();
        Project project = I.make(Project.class);

        try {
            Files.write(file, text.getBytes(project.getEncoding()));

            return file;
        } catch (IOException e) {
            I.make(UserInterface.class).error("Fail to write [", file, "]");
            throw I.quiet(e);
        }
    }

    /**
     * Tests whether a file exists.
     * <p>
     * The {@code options} parameter may be used to indicate how symbolic links are handled for the
     * case that the file is a symbolic link. By default, symbolic links are followed. If the option
     * {@link LinkOption#NOFOLLOW_LINKS NOFOLLOW_LINKS} is present then symbolic links are not
     * followed.
     * <p>
     * Note that the result of this method is immediately outdated. If this method indicates the
     * file exists then there is no guarantee that a subsequence access will succeed. Care should be
     * taken when using this method in security sensitive applications.
     *
     * @param path the path to the file to test
     * @param options options indicating how symbolic links are handled .
     * @return {@code true} if the file exists; {@code false} if the file does not exist or its
     *         existence cannot be determined.
     * @throws SecurityException In the case of the default provider, the
     *             {@link SecurityManager#checkRead(String)} is invoked to check read access to the
     *             file.
     * @see #notExists
     */
    public static boolean exist(Path path) {
        return path != null && Files.exists(path);
    }

    /**
     * <p>
     * Helper method to create file.
     * </p>
     * 
     * @param file A file.
     * @return A created file.
     */
    public static Path createFile(Path file) {
        if (Files.isRegularFile(file) == false) {
            try {
                Files.createDirectories(file.getParent());
                Files.createFile(file);
            } catch (IOException e) {
                throw I.quiet(e);
            }
        }
        return file;
    }

    /**
     * <p>
     * Helper method to create directory.
     * </p>
     * 
     * @param path A directory.
     * @return A created directory.
     */
    public static Path createDirectory(Path path) {
        if (Files.isDirectory(path) == false) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw I.quiet(e);
            }
        }
        return path;
    }

    /**
     * @param ignore
     * @return
     */
    public static List<String> readLines(Path file) {
        try {
            return Files.readAllLines(file);
        } catch (IOException e) {
            return new ArrayList();
        }
    }

}
