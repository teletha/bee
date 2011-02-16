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
package bee;

import static java.nio.file.FileVisitResult.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import ezbean.I;

/**
 * @version 2011/02/15 15:51:49
 */
public class PathSet implements Iterable<Path> {

    /** The base path. */
    private final Path base;

    /** The actual filter set for directory only matcher. */
    private Set<Wildcard> includeFile = new CopyOnWriteArraySet();

    /** The actual filter set for directory only matcher. */
    private Set<Wildcard> includeDirectory = new CopyOnWriteArraySet();

    /** The actual filter set for directory only matcher. */
    private Set<Wildcard> excludeFile = new CopyOnWriteArraySet();

    /** The actual filter set for directory only matcher. */
    private Set<Wildcard> excludeDirectory = new CopyOnWriteArraySet();

    /**
     * @param base
     */
    public PathSet(String base) {
        this(Paths.get(base));
    }

    /**
     * @param base
     */
    public PathSet(Path base) {
        this.base = base;
    }

    /**
     * <p>
     * Specify the file name patterns that you want to include.
     * </p>
     * 
     * @param patterns A include file patterns.
     * @return {@link PathSet} instance to chain API.
     */
    public PathSet include(String... patterns) {
        return parse(patterns, includeFile, includeDirectory);
    }

    /**
     * <p>
     * Specify the directory name patterns that you want to exclude.
     * </p>
     * 
     * @param patterns A exclude directory patterns.
     * @return {@link PathSet} instance to chain API.
     */
    public PathSet exclude(String... patterns) {
        return parse(patterns, excludeFile, excludeDirectory);
    }

    /**
     * <p>
     * Helper method to parse the specified patterns and store it for the suitable collection.
     * </p>
     */
    private PathSet parse(String[] patterns, Set<Wildcard> forFile, Set<Wildcard> forDirectory) {
        for (String pattern : patterns) {
            String[] parsed = pattern.replace(File.separatorChar, '/')
                    .replaceAll("\\*{3,}", "**")
                    .replaceAll("\\*\\*([^/])", "**/*$1")
                    .replaceAll("([^/])\\*\\*", "$1*/**")
                    .replace("/$", "/**")
                    .replace("^/", "")
                    .replaceAll("\\*\\*/\\*\\*", "**")
                    .split("/");

            if (parsed.length != 2) {
                throw new IllegalArgumentException("The pattern '" + pattern + "' is too complicated. PathSet accepts the following patterns only:\r\n  **/file.name\r\n  dir name/**");
            } else {
                if (parsed[0].equals("**")) {
                    forFile.add(new Wildcard(parsed[1]));
                } else if (parsed[1].equals("**")) {
                    forDirectory.add(new Wildcard(parsed[0]));
                }
            }
        }

        // API chain
        return this;
    }

    /**
     * <p>
     * Exclude the default
     * </p>
     * 
     * @return {@link PathSet} instance to chain API.
     */
    public PathSet excludeDefaults() {
        return exclude("**/.*", ".*/**", "CVS/**", "SCCS/**");
    }

    public void copyTo(Path dist) {

    }

    public void moveTo(Path dist) {
        copyTo(dist);
        delete();
    }

    public void delete() {

    }

    /**
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<Path> iterator() {
        return Collections.EMPTY_LIST.iterator();
    }

    public void scan(FileVisitor<Path> vistor) {
        boolean unsort = includeFile.isEmpty() && includeDirectory.isEmpty() && excludeDirectory.isEmpty() && excludeFile.isEmpty();

        try {
            Files.walkFileTree(base, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, unsort ? vistor
                    : new Traveler(vistor, excludeDirectory, excludeFile, includeFile, includeDirectory));
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * @version 2011/02/15 15:49:01
     */
    private final class Traveler implements FileVisitor<Path> {

        private final boolean hasInclude;

        /** The simple file include patterns. */
        private final Wildcard[] includeFile;

        /** The simple directory include patterns. */
        private final Wildcard[] includeDirectory;

        /** We should exclude something? */
        private final int includeFileSize;

        /** We should exclude something? */
        private final boolean hasIncludeDirectory;

        /** The simple file exclude patterns. */
        private final Wildcard[] excludeFile;

        /** The simple directory exclude patterns. */
        private final Wildcard[] excludeDirectory;

        /** We should exclude something? */
        private final int excludeFileSize;

        /** We should exclude something? */
        private final boolean hasExcludeDirectory;

        /** The actual file visitor. */
        private final FileVisitor<Path> delegator;

        /** The current depth of file system. */
        private int depth = 0;

        /** The last depth that we should include all files. */
        private int depthForDirectoryIncluding = Integer.MAX_VALUE;

        /**
         * @param delegator
         */
        private Traveler(FileVisitor<Path> delegator, Set<Wildcard> excludeDirectory, Set<Wildcard> excludeFile, Set<Wildcard> includeFile, Set<Wildcard> includeDirectory) {
            this.delegator = delegator;
            this.includeFile = includeFile.toArray(new Wildcard[includeFile.size()]);
            this.excludeFile = excludeFile.toArray(new Wildcard[excludeFile.size()]);
            this.includeDirectory = includeDirectory.toArray(new Wildcard[includeDirectory.size()]);
            this.excludeDirectory = excludeDirectory.toArray(new Wildcard[excludeDirectory.size()]);

            includeFileSize = this.includeFile.length;
            excludeFileSize = this.excludeFile.length;
            hasIncludeDirectory = this.includeDirectory.length != 0;
            hasExcludeDirectory = this.excludeDirectory.length != 0;
            hasInclude = includeFileSize != 0 || hasIncludeDirectory;
        }

        /**
         * @see java.nio.file.FileVisitor#visitFile(java.lang.Object,
         *      java.nio.file.attribute.BasicFileAttributes)
         */
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            String name = file.getName().toString();

            // Check excluding because of excluding pattern has high priority.
            for (int i = 0; i < excludeFileSize; i++) {
                if (excludeFile[i].match(name)) {
                    return CONTINUE; // Discard the current file.
                }
            }

            if (!hasInclude) {
                // We must pick up all files.
                return delegator.visitFile(file, attrs);
            } else {
                // We must pick up the specified files and the others are discarded.

                // Chech whether the current directory is unconditional including or not.
                if (depthForDirectoryIncluding <= depth) {
                    return delegator.visitFile(file, attrs);
                }

                // Check file including because some files are discarded.
                for (int i = 0; i < includeFileSize; i++) {
                    if (includeFile[i].match(name)) {
                        return delegator.visitFile(file, attrs);
                    }
                }

                // Any pattern doesn't match the curretn file, so we must discard it.
                return CONTINUE;
            }
        }

        /**
         * @see java.nio.file.FileVisitor#visitFileFailed(java.lang.Object, java.io.IOException)
         */
        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            return CONTINUE;
        }

        /**
         * @see java.nio.file.FileVisitor#preVisitDirectory(java.lang.Object,
         *      java.nio.file.attribute.BasicFileAttributes)
         */
        @Override
        public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attrs) throws IOException {
            depth++;

            if (hasExcludeDirectory) {
                String name = directory.getName().toString();

                for (Wildcard wildcard : excludeDirectory) {
                    if (wildcard.match(name)) {
                        return SKIP_SUBTREE;
                    }
                }
            }

            if (hasIncludeDirectory) {
                String name = directory.getName().toString();

                for (Wildcard wildcard : includeDirectory) {
                    if (wildcard.match(name)) {
                        depthForDirectoryIncluding = depth; // record depth

                        // delegate
                        return delegator.preVisitDirectory(directory, attrs);
                    }
                }
            }

            // delegate
            return delegator.preVisitDirectory(directory, attrs);
        }

        /**
         * @see java.nio.file.FileVisitor#postVisitDirectory(java.lang.Object, java.io.IOException)
         */
        @Override
        public FileVisitResult postVisitDirectory(Path directory, IOException exc) throws IOException {
            if (depth == depthForDirectoryIncluding) {
                depthForDirectoryIncluding = Integer.MAX_VALUE; // reset
            }

            depth--;
            return CONTINUE;
        }
    }
}
