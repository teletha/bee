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

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import antibug.CleanRoom;

/**
 * @version 2011/03/15 18:30:55
 */
public class JarArchiverTest {

    @Rule
    @ClassRule
    public static final CleanRoom room = new CleanRoom();

    @Test
    public void file() throws Exception {
        Path file = room.locateFile("root/file");
        Path output = room.locateAbsent("out");

        JarArchiver jar = new JarArchiver();
        jar.add(file.getParent());
        jar.pack(output);

        try (FileSystem system = FileSystems.newFileSystem(output, null)) {
            Path archive = system.getPath("/");

            assert Files.exists(archive.resolve("file"));
            assert Files.isRegularFile(archive.resolve("file"));
        }
    }

    @Test
    public void directory() throws Exception {
        Path root = room.locateDirectory("root", $ -> {
            $.file("file");
            $.dir("directory", () -> {
                $.file("file");
            });
        });
        Path output = room.locateAbsent("out");

        JarArchiver jar = new JarArchiver();
        jar.add(root);
        jar.pack(output);

        try (FileSystem system = FileSystems.newFileSystem(output, null)) {
            Path archive = system.getPath("/");

            assert Files.exists(archive.resolve("file"));
            assert Files.exists(archive.resolve("directory"));
            assert Files.isDirectory(archive.resolve("directory"));
            assert Files.exists(archive.resolve("directory/file"));
            assert Files.isRegularFile(archive.resolve("directory/file"));
        }
    }
}
