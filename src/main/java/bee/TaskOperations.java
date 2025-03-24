/*
 * Copyright (C) 2025 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;
import java.util.function.UnaryOperator;

import bee.api.Project;
import kiss.I;
import kiss.WiseFunction;
import kiss.XML;
import psychopath.Directory;
import psychopath.File;
import psychopath.Option;

public class TaskOperations {

    /**
     * Get the current processing {@link Project}.
     * 
     * @return
     */
    public static final Project project() {
        return I.make(Project.class);
    }

    /**
     * Get the current {@link UserInterface}.
     * 
     * @return
     */
    public static final UserInterface ui() {
        return I.make(UserInterface.class);
    }

    /**
     * Utility method for task.
     * 
     * @param path
     */
    public static final Directory makeDirectory(Directory base, String path) {
        Directory directory = base.directory(path);

        if (directory.isAbsent()) {
            directory.create();

            ui().info("Make directory [", directory.absolutize(), "]");
        }
        return directory;
    }

    /**
     * Utility method to write xml file.
     * 
     * @param file A file path to write.
     * @param xml A file contents.
     */
    public static final File makeFile(File file, XML xml) {
        try (BufferedWriter writer = file.newBufferedWriter()) {
            xml.to(writer);

            ui().info("Make file [", file.absolutize(), "]");
        } catch (IOException e) {
            throw I.quiet(e);
        }
        return file;
    }

    /**
     * Utility method to write property file.
     * 
     * @param path A file path to write.
     * @param properties A file contents.
     */
    public static final File makeFile(File path, Properties properties) {
        path.parent().create();

        try {
            properties.store(path.newOutputStream(), "");

            ui().info("Make file [", path.absolutize(), "]");
        } catch (IOException e) {
            throw I.quiet(e);
        }
        return path;
    }

    /**
     * Utility method to write file.
     * 
     * @param path A file path to write.
     * @param content A file content.
     */
    public static final File makeFile(String path, String content) {
        if (path == null) {
            throw new Fail("Input file is null.");
        }
        return makeFile(project().getRoot().file(path), content);
    }

    /**
     * Utility method to write file.
     * 
     * @param file A file path to write.
     * @param content A file content.
     */
    public static final File makeFile(File file, String content) {
        return makeFile(file, Arrays.asList(content.split("\\R")));
    }

    /**
     * Utility method to write file.
     * 
     * @param path A file path to write.
     * @param content A file content.
     */
    public static final File makeFile(String path, Iterable<String> content) {
        if (path == null) {
            throw new Fail("Input file is null.");
        }
        return makeFile(project().getRoot().file(path), content);
    }

    /**
     * Utility method to write file.
     * 
     * @param file A file path to write.
     * @param content A file content.
     */
    public static final File makeFile(File file, Iterable<String> content) {
        if (file == null) {
            throw new Fail("Input file is null.");
        }

        file.text(content);

        Iterator<String> iterator = content.iterator();
        if (iterator.hasNext() && iterator.next().startsWith("#!")) {
            file.text(x -> x.replaceAll("\\R", "\n"));
        }

        ui().info("Make file [", file.absolutize(), "]");

        return file;
    }

    /**
     * Utility method to write file.
     * 
     * @param file A file path to write.
     * @param replacer A file content replacer.
     */
    public static final File makeFile(File file, WiseFunction<String, String> replacer) {
        return makeFile(file, file.lines().map(replacer).toList());
    }

    /**
     * Utility method to delete file.
     * 
     * @param path A file path to delete.
     */
    public static final void deleteFile(String path) {
        if (path != null) {
            deleteFile(project().getRoot().file(path));
        }
    }

    /**
     * Utility method to delete file.
     * 
     * @param file A file to delete.
     */
    public static final void deleteFile(File file) {
        if (file != null && file.isPresent()) {
            file.delete();
            ui().info("Delete file [", file.absolutize(), "]");
        }
    }

    /**
     * Utility method to delete directory.
     * 
     * @param path A directory path to delete.
     */
    public static final void deleteDirectory(String path) {
        if (path != null) {
            deletedirectory(project().getRoot().directory(path));
        }
    }

    /**
     * Utility method to delete directory.
     * 
     * @param dir A directory to delete.
     */
    public static final void deletedirectory(Directory dir) {
        if (dir != null && dir.isPresent()) {
            dir.delete();
            ui().info("Delete directory [", dir.absolutize(), "]");
        }
    }

    /**
     * Utility method to delete file.
     * 
     * @param from A file to copy.
     * @param to A destination.
     */
    public static final void copyFile(File from, File to) {
        if (from == null) {
            throw new Fail("The specified file is null.");
        }

        if (from.isAbsent()) {
            throw new Fail("File [" + from + "] is not found.");
        }

        from.copyTo(to);
        ui().info("Copy file from [", from.absolutize(), "] to [", to.absolutize() + "]");
    }

    /**
     * Utilitu method to unpack archive.
     * 
     * @param from
     * @param to
     */
    public static final void pack(Directory from, File to) {
        pack(from, to, UnaryOperator.identity());
    }

    /**
     * Utilitu method to unpack archive.
     * 
     * @param from
     * @param to
     */
    public static final void pack(Directory from, File to, UnaryOperator<Option> options) {
        if (from == null) {
            throw new Fail("The specified file is null.");
        }

        if (from.isAbsent()) {
            throw new Fail("File [" + from + "] is not found.");
        }

        from.trackPackingTo(to, options).to(progress -> {
            ui().trace("Packing ", from.name(), " to ", to, " (", progress.rateByFiles(), "%)");
        }, e -> {
            ui().error(e);
        }, () -> {
            ui().info("Packed ", from.name(), " to ", to);
        });
    }

    /**
     * Utilitu method to unpack archive.
     * 
     * @param from
     * @param to
     */
    public static final void unpack(File from, Directory to) {
        unpack(from, to, UnaryOperator.identity());
    }

    /**
     * Utilitu method to unpack archive.
     * 
     * @param from
     * @param to
     */
    public static final void unpack(File from, Directory to, UnaryOperator<Option> options) {
        if (from == null) {
            throw new Fail("The specified file is null.");
        }

        if (from.isAbsent()) {
            throw new Fail("File [" + from + "] is not found.");
        }

        from.trackUnpackingTo(to, options).to(progress -> {
            ui().trace("Unpacking ", from.name(), " to ", to, " (", progress.rateByFiles(), "%)");
        }, e -> {
            ui().error(e);
        }, () -> {
            ui().info("Unpacked ", from.name(), " to ", to);
        });
    }

    /**
     * Utility method to check file.
     * 
     * @param path A file path to check.
     */
    public static final boolean checkFile(String path) {
        if (path == null) {
            return false;
        }
        return checkFile(project().getRoot().file(path));
    }

    /**
     * Utility method to check file.
     * 
     * @param file A file to check.
     */
    public static final boolean checkFile(File file) {
        return file != null && file.isPresent();
    }
}
