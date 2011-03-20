/*
 * Copyright (C) 2011 Nameless Production Committee.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bee.task;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;

import ezunit.CleanRoom;

/**
 * @version 2011/03/15 18:30:55
 */
public class JarArchiverTest {

    @Rule
    public static final CleanRoom room = new CleanRoom();

    @Test
    public void file() throws Exception {
        Path file = room.locateFile("root/file");
        Path output = room.locateAbsent("out");

        Jar jar = new Jar();
        jar.add(file.getParent());
        jar.pack(output);

        Path archive = room.locateArchive(output);
        assertTrue(Files.exists(archive.resolve("file")));
        assertTrue(Files.isRegularFile(archive.resolve("file")));
    }

    @Test
    public void directory() throws Exception {
        Path file1 = room.locateFile("root/file");
        room.locateFile("root/directory/file");
        Path output = room.locateAbsent("out");

        Jar jar = new Jar();
        jar.add(file1.getParent());
        jar.pack(output);

        Path archive = room.locateArchive(output);
        assertTrue(Files.exists(archive.resolve("file")));
        assertTrue(Files.exists(archive.resolve("directory")));
        assertTrue(Files.isDirectory(archive.resolve("directory")));
        assertTrue(Files.exists(archive.resolve("directory/file")));
        assertTrue(Files.isRegularFile(archive.resolve("directory/file")));
    }
}
