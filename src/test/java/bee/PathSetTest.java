/*
 * Copyright (C) 2010 Nameless Production Committee.
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
package bee;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.Test;

/**
 * @version 2010/06/14 17:43:01
 */
public class PathSetTest {

    private File base = new File("src");

    @Test
    public void iterate() throws Exception {
        PathSet set = new PathSet("src");

        for (Path path : set) {
            System.out.println(path);
        }

        set.scan(new SimpleFileVisitor<Path>() {

            /**
             * @see java.nio.file.SimpleFileVisitor#visitFile(java.lang.Object,
             *      java.nio.file.attribute.BasicFileAttributes)
             */
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                System.out.println(file);

                return super.visitFile(file, attrs);
            }

        });
    }
}
