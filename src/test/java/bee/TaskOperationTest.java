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

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import psychopath.File;

class TaskOperationTest extends TaskTestBase {

    @Test
    void makeFile() {
        File file = locateFile("file");
        assert file.isAbsent();

        noop.makeFile(file, "text");

        assert file.isPresent();
        assert file.text().equals("text");
    }

    @Test
    void makeFileMultiLined() {
        File file = locateFile("file");
        assert file.isAbsent();

        noop.makeFile(file, """
                A
                B
                """);

        assert file.isPresent();
        assert file.text().equals("A" + Platform.EOL + "B");
    }

    @Test
    void makeStringFileMultiLined() {
        File file = locateFile("file");
        assert file.isAbsent();

        noop.makeFile("file", """
                A
                B
                """);

        assert file.isPresent();
        assert file.text().equals("A" + Platform.EOL + "B");
    }

    @Test
    void makeFileMultiLinedIterable() {
        File file = locateFile("file");
        assert file.isAbsent();

        noop.makeFile(file, List.of("A", "B"));

        assert file.isPresent();
        assert file.text().equals("A" + Platform.EOL + "B");
    }

    @Test
    void makeStringFileMultiLinedIterable() {
        File file = locateFile("file");
        assert file.isAbsent();

        noop.makeFile("file", List.of("A", "B"));

        assert file.isPresent();
        assert file.text().equals("A" + Platform.EOL + "B");
    }

    @Test
    void makeNullFile() {
        Assertions.assertThrows(Fail.class, () -> noop.makeFile((File) null, "text"));
        Assertions.assertThrows(Fail.class, () -> noop.makeFile((String) null, "text"));
    }

    @Test
    void makeNullFileMultiLinedIterable() {
        Assertions.assertThrows(Fail.class, () -> noop.makeFile((File) null, List.of("A", "B")));
        Assertions.assertThrows(Fail.class, () -> noop.makeFile((String) null, List.of("A", "B")));
    }

    @Test
    void deleteFile() {
        File file = locateFile("file").create();
        assert file.isPresent();

        noop.deleteFile(file);

        assert file.isAbsent();
    }

    @Test
    void deleteStringFile() {
        File file = locateFile("file").create();
        assert file.isPresent();

        noop.deleteFile("file");

        assert file.isAbsent();
    }

    @Test
    void deleteNullFile() {
        noop.deleteFile((File) null);
        noop.deleteFile((String) null);
    }

    @Test
    void checkFile() {
        File file = locateFile("file");
        assert noop.checkFile(file) == false;

        file.create();
        assert noop.checkFile(file) == true;
    }

    @Test
    void checkStringFile() {
        assert noop.checkFile("file") == false;

        locateFile("file").create();

        assert noop.checkFile("file") == true;
    }

    @Test
    void checkNullFile() {
        assert noop.checkFile((File) null) == false;
        assert noop.checkFile((String) null) == false;
    }

    @Test
    void copyFile() {
        File input = locateFile("input").create();
        File output = locateFile("output");

        assert input.isPresent();
        assert output.isAbsent();

        noop.copyFile(input, output);

        assert input.isPresent();
        assert output.isPresent();
    }

    @Test
    void copyAbsentFile() {
        File input = locateFile("input");
        File output = locateFile("output");

        Assertions.assertThrows(Fail.class, () -> noop.copyFile(input, output));
    }

    @Test
    void copyNullFile() {
        File input = null;
        File output = locateFile("output");

        Assertions.assertThrows(Fail.class, () -> noop.copyFile(input, output));
    }
}