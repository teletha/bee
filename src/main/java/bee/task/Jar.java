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
import java.util.jar.Pack200;
import java.util.jar.Pack200.Packer;

import ezbean.I;

/**
 * @version 2011/03/15 18:21:32
 */
public class Jar {

    /** The packer. */
    private final Packer packer = Pack200.newPacker();

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
            try {
                // Location must exist
                if (Files.notExists(location)) {
                    Files.createDirectories(location.getParent());
                    Files.createFile(location);
                }

                // Location must be file.
                if (!Files.isRegularFile(location)) {
                    throw new IllegalArgumentException("'" + location + "' must be regular file.");
                }

                OutputStream output = Files.newOutputStream(location);
                JarOutputStream jar = new JarOutputStream(output);

                try {
                    // Start packing
                    for (Entry entry : entries) {
                        I.walk(entry.base, new Scanner(jar, entry.base), entry.patterns);
                    }

                    // Finish packing.
                    jar.finish();
                } finally {

                    jar.close();
                    output.close();
                    System.out.println("close jar");
                }
            } catch (IOException e) {
                throw I.quiet(e);
            }
        }
    }

    /**
     * @version 2011/03/15 18:07:46
     */
    private static class Entry {

        /** The base directory. */
        private final Path base;

        /** The patterns. */
        private final String[] patterns;

        /**
         * @param base
         */
        public Entry(Path base, String... patterns) {
            this.base = base;
            this.patterns = patterns;
        }

    }

    /**
     * @version 2011/03/17 15:43:50
     */
    private static class Scanner extends SimpleFileVisitor<Path> {

        /** The actual jar stream. */
        private final JarOutputStream jar;

        /** The current scanning information. */
        private final Path base;

        /**
         * @param jar
         * @param base
         */
        private Scanner(JarOutputStream jar, Path base) {
            this.jar = jar;
            this.base = base;
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
            entry.setTime(Files.getLastModifiedTime(file).toMillis());

            // create file entry
            jar.putNextEntry(entry);

            InputStream input = Files.newInputStream(file);

            try {
                // copy data
                I.copy(input, jar, false);

            } finally {
                System.out.println("close " + file);
                input.close();
            }

            // finish
            jar.closeEntry();

            // continue
            return FileVisitResult.CONTINUE;
        }
    }
}
