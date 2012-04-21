/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.util;

import static java.nio.file.FileVisitResult.*;

import java.io.BufferedOutputStream;
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
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import kiss.Disposable;
import kiss.I;
import bee.Platform;

/**
 * @version 2011/03/17 14:01:01
 */
public class ZipArchiver {

    /** The default encoding. */
    protected Charset encoding = Platform.Encoding;

    /** The default manifest. */
    protected Manifest manifest;

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
            location = location.toAbsolutePath();

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

                Archiver archiver = new Archiver(location, encoding);

                if (manifest != null) {
                    ZipEntry entry = new JarEntry(JarFile.MANIFEST_NAME);
                    archiver.putNextEntry(entry);
                    manifest.write(new BufferedOutputStream(archiver));
                    archiver.closeEntry();
                }

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
        private Archiver(Path destination, Charset encoding) throws IOException {
            super(Files.newOutputStream(destination), encoding);
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
