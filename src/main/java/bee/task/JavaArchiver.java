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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import ezbean.I;

/**
 * @version 2011/03/15 18:21:32
 */
public class JavaArchiver {

    /** The path entries. */
    private final List<Entry> entries = new ArrayList();

    /**
     * <p>
     * Add pattern matching path.
     * </p>
     * 
     * @param base A base path.
     * @param patterns "glob" include/exclude patterns.
     */
    public void add(Path base, String... patterns) {
        if (base != null) {
            entries.add(new Entry(base, patterns));
        }
    }

    /**
     * <p>
     * Pack all resources.
     * </p>
     * 
     * @param location
     */
    public void pack(Path location) {
        if (location != null) {
            OutputStream output = null;

            try {
                if (Files.notExists(location)) {
                    Files.createDirectories(location.getParent());
                    Files.createFile(location);
                }

                if (!Files.isRegularFile(location)) {
                    throw new IllegalArgumentException("'" + location + "' must be regular file.");
                }

                output = Files.newOutputStream(location);

                JarOutputStream stream = null;

                try {
                    stream = new JarOutputStream(output);

                    for (Entry entry : entries) {
                        entry.output = stream;
                        I.walk(entry.base, entry, entry.patterns);
                    }
                } finally {
                    stream.finish();
                    stream.close();
                }
            } catch (IOException e) {
                throw I.quiet(e);
            } finally {
                I.quiet(output);
            }
        }
    }

    /**
     * @version 2011/03/15 18:07:46
     */
    private static class Entry extends SimpleFileVisitor<Path> {

        /** The base directory. */
        private final Path base;

        /** The patterns. */
        private final String[] patterns;

        private JarOutputStream output;

        /**
         * @param base
         */
        public Entry(Path base, String... patterns) {
            this.base = base;
            this.patterns = patterns;
        }

        /**
         * @see java.nio.file.SimpleFileVisitor#preVisitDirectory(java.lang.Object,
         *      java.nio.file.attribute.BasicFileAttributes)
         */
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            JarEntry entry = new JarEntry(base.relativize(dir).toString());
            return super.preVisitDirectory(dir, attrs);
        }

        /**
         * @see java.nio.file.SimpleFileVisitor#visitFile(java.lang.Object,
         *      java.nio.file.attribute.BasicFileAttributes)
         */
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            JarEntry entry = new JarEntry(base.relativize(file).toString());
            output.putNextEntry(entry);

            InputStream input = null;

            try {
                input = Files.newInputStream(file);
                I.copy(input, output, false);
            } finally {
                I.quiet(input);
                output.closeEntry();
            }

            return FileVisitResult.CONTINUE;
        }
    }
}
