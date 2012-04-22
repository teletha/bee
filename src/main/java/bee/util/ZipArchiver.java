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
            entries.add(new Entry("", base, patterns));
        }
    }

    /**
     * <p>
     * Add the target file.
     * </p>
     * 
     * @param base A base directory path in this zip file.
     * @param target A target file to archive.
     */
    public void add(String base, Path target) {
        if (target != null) {
            entries.add(new Entry(base, target));
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
                        archiver.directory = entry.directory;
                        archiver.base = entry.base;

                        // scan entry
                        if (Files.isDirectory(entry.base)) {
                            I.walk(entry.base, archiver, entry.patterns);
                        } else {
                            archiver.add(entry.directory + entry.base.getFileName(), entry.base, Files.readAttributes(entry.base, BasicFileAttributes.class));
                        }
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
     * @version 2012/04/22 11:04:40
     */
    private static class Archiver extends ZipOutputStream implements FileVisitor<Path>, Disposable {

        /** The base directory path. */
        private String directory;

        /** The base path. */
        private Path base;

        /**
         * @param output
         */
        private Archiver(Path destination, Charset encoding) throws IOException {
            super(Files.newOutputStream(destination), encoding);
        }

        /**
         * <p>
         * Add archive entry.
         * </p>
         * 
         * @param path
         * @param attrs
         */
        private void add(String path, Path file, BasicFileAttributes attrs) {
            try {
                ZipEntry entry = new ZipEntry(path);
                entry.setSize(attrs.size());
                entry.setTime(attrs.lastModifiedTime().toMillis());
                putNextEntry(entry);

                // copy data
                I.copy(Files.newInputStream(file), this, true);
                closeEntry();
            } catch (IOException e) {
                // ignore
            }
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
            add(directory + base.relativize(file).toString().replace(File.separatorChar, '/'), file, attrs);

            // API definition
            return CONTINUE;
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

        /** The directory path in archive. */
        private final String directory;

        /** The base directory. */
        private final Path base;

        /** The patterns. */
        private final String[] patterns;

        /**
         * @param base
         */
        public Entry(String directory, Path base, String... patterns) {
            if (directory == null) {
                directory = "";
            }
            directory = directory.replace(File.separatorChar, '/');

            this.directory = directory.length() == 0 || directory.endsWith("/") ? directory : directory.concat("/");
            this.base = base;
            this.patterns = patterns;
        }
    }
}
