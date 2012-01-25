/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package testament;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;

import kiss.I;
import kiss.model.ClassUtil;

/**
 * @version 2011/03/08 18:35:36
 */
public class CleanRoomTest {

    @Rule
    public static final CleanRoom room = new CleanRoom();

    @Test
    public void locateFile() {
        Path file = room.locateFile("empty");

        assert Files.exists(file);
        assert Files.isRegularFile(file);

    }

    @Test
    public void locateArchive() {
        Path file = room.locateFile("jar");
        I.copy(ClassUtil.getArchive(Test.class), file);

        file = room.locateArchive("jar");
        assert Files.exists(file.resolve("org/junit/Test.class"));
        assert Files.isRegularFile(file.resolve("org/junit/Test.class"));
        assert Files.exists(file.resolve("org/junit"));
        assert Files.isDirectory(file.resolve("org/junit"));
        assert Files.notExists(file.resolve("not-exists"));
    }

    @Test
    public void locateDirectoryFromAbsent() {
        Path file = room.locateDirectory("absent");

        assert Files.exists(file);
        assert Files.isDirectory(file);
    }

    @Test
    public void locateDirectoryFromPresent() {
        Path file = room.locateDirectory("dir");

        assert Files.exists(file);
        assert Files.isDirectory(file);
    }

    @Test
    public void locateAbsent() {
        Path file = room.locateAbsent("absent.txt");

        assert Files.notExists(file);
        assert !Files.isRegularFile(file);
        assert !Files.isDirectory(file);
    }

    @Test
    public void locatePresentFile() {
        Path file = room.locateAbsent("present.txt");

        // the specified file doesn't exist yet
        assert !Files.exists(file);

        // create file
        file = room.locateFile("present.txt");
        assert Files.exists(file);

        // the file has already existed
        file = room.locateFile("present.txt");
        assert Files.exists(file);
    }

    @Test
    public void locatedFileCanDelete() throws Exception {
        Path file = room.locateFile("empty");

        assert Files.exists(file);
        assert Files.deleteIfExists(file);
        assert Files.notExists(file);
    }
}
