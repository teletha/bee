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

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
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
    public void jar() throws Exception {
        Path file = room.locateFile("resources/file");
        Path path = room.locateAbsent("test.jar");
        assertTrue(Files.notExists(path));

        JavaArchiver jar = new JavaArchiver();
        jar.add(file.getParent());
        jar.pack(path);

        assertTrue(Files.exists(path));

        FileSystem system = FileSystems.newFileSystem(path, null);
        assertTrue(Files.exists(system.getPath("file")));
    }
}
