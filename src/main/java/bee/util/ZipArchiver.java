/*
 * Copyright (C) 2015 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import bee.Platform;
import kiss.Disposable;
import kiss.I;

/**
 * @version 2015/07/14 2:48:03
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
    public void add(PathSet set) {
        if (set != null) {
            for (PathPattern pattern : set) {
                add(pattern);
            }
        }
    }

    /**
     * <p>
     * Add pattern matching path.
     * </p>
     * 
     * @param base A base path.
     * @param patterns "glob" include/exclude patterns.
     */
    public void add(PathPattern path) {
        if (path != null) {
            entries.add(new Entry("", path.base, path.patterns.toArray(new String[path.patterns.size()])));
        }
    }

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
                        I.walk(entry.base, entry.patterns).forEach(file -> {
                            try {
                                String path = archiver.directory + archiver.base.relativize(file)
                                        .toString()
                                        .replace(File.separatorChar, '/');
                                BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);

                                ZipEntry zip = new ZipEntry(path);
                                zip.setSize(attrs.size());
                                zip.setCreationTime(attrs.creationTime());
                                zip.setLastAccessTime(attrs.lastAccessTime());
                                zip.setLastModifiedTime(attrs.lastModifiedTime());

                                archiver.putNextEntry(zip);
                                I.copy(Files.newInputStream(file), archiver, true);
                                archiver.closeEntry();
                            } catch (IOException e) {
                                // ignore
                            }
                        });
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
     * <p>
     * Pack all resources.
     * </p>
     * 
     * @param location
     */
    public static void unpack(Path input, Path output) {
        try (ZipFile zip = new ZipFile(input.toFile(), Platform.Encoding)) {
            Enumeration<? extends ZipEntry> items = zip.entries();

            while (items.hasMoreElements()) {
                ZipEntry entry = items.nextElement();
                Path path = output.resolve(entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(path);
                } else {
                    Files.createDirectories(path.getParent());
                    I.copy(zip.getInputStream(entry), Files.newOutputStream(path), false);
                }
            }
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * @version 2015/06/23 21:21:57
     */
    private static class Archiver extends ZipOutputStream implements Disposable {

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
         * {@inheritDoc}
         */
        @Override
        public void close() throws IOException {
            // super.close();
        }

        /**
         * {@inheritDoc}
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
     * @version 2015/06/23 21:21:53
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
