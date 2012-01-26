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

import static java.nio.file.FileVisitResult.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import kiss.Disposable;
import kiss.I;

/**
 * @version 2011/03/17 14:01:01
 */
public abstract class ZipArchiver {

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

                Archiver archiver = new Archiver(location);

                try {
                    for (Entry entry : entries) {
                        archiver.base = entry.base;

                        // scan entry
                        I.walk(entry.base, archiver, entry.patterns);
                    }
                } finally {
                    archiver.dispose();
                }
            } catch (IOException e) {
                throw I.quiet(e);
            }
        }
    }

    /**
     * @version 2011/03/20 15:43:35
     */
    private static class Archiver extends ZipOutputStream implements FileVisitor<Path>, Disposable {

        /** The base path. */
        private Path base;

        /**
         * @param output
         */
        private Archiver(Path destination) throws IOException {
            super(Files.newOutputStream(destination), Charset.defaultCharset());
        }

        /**
         * @see java.nio.file.FileVisitor#preVisitDirectory(java.lang.Object,
         *      java.nio.file.attribute.BasicFileAttributes)
         */
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            return CONTINUE;
        }

        /**
         * @see java.nio.file.FileVisitor#visitFile(java.lang.Object,
         *      java.nio.file.attribute.BasicFileAttributes)
         */
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            try {
                ZipEntry entry = new ZipEntry(base.relativize(file).toString().replace(File.separatorChar, '/'));
                entry.setSize(attrs.size());
                entry.setTime(attrs.lastModifiedTime().toMillis());
                putNextEntry(entry);

                // copy data
                I.copy(Files.newInputStream(file), this, true);
                closeEntry();

                // API definition
                return CONTINUE;
            } catch (ZipException e) {
                // ignore
                return CONTINUE;
            }
        }

        /**
         * @see java.nio.file.FileVisitor#visitFileFailed(java.lang.Object, java.io.IOException)
         */
        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            // API definition
            return CONTINUE;
        }

        /**
         * @see java.nio.file.FileVisitor#postVisitDirectory(java.lang.Object, java.io.IOException)
         */
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            // API definition
            return CONTINUE;
        }

        /**
         * @see java.util.zip.ZipOutputStream#close()
         */
        @Override
        public void close() throws IOException {
            // super.close();
        }

        /**
         * @see ezbean.Disposable#dispose()
         */
        @Override
        public void dispose() {
            try {
                super.close();
            } catch (IOException e) {
                throw I.quiet(e);
            }
        }

        /**
         * <p>
         * Helper method to create a {@link ZipEntry} from {@link Path} information.
         * </p>
         * 
         * @param path
         * @param attr
         * @return
         */
        private ZipEntry createEntry(Path path, BasicFileAttributes attr) {
            boolean directory = attr.isDirectory();
            String name = path.toString().replace(File.separatorChar, '/');

            if (directory) {
                name = name.concat("/");
            }

            ZipEntry entry = new ZipEntry(name);
            entry.setSize(directory ? 0 : attr.size());
            entry.setTime(attr.lastModifiedTime().toMillis());

            // API definition
            return entry;
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
}
