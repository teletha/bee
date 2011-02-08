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
 * @version 2010/12/19 12:04:07
 */
public class PathSet implements Iterable<Path> {

    /** The base path. */
    private final Path base;

    /** The actual filter set for file only matcher. */
    private Set<FilePathMatcher> files = new CopyOnWriteArraySet();

    /** The actual filter set for file only matcher. */
    private Set<DirectoryPathMatcher> direcories = new CopyOnWriteArraySet();

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

    public PathSet include(String... patterns) {
        for (String pattern : patterns) {
            add(pattern);
        }
        return this;
    }

    public PathSet exclude(String... patterns) {
        return this;
    }

    public PathSet excludeDefault(boolean exclusion) {
        return this;
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
        boolean unsort = files.isEmpty() && direcories.isEmpty();

        try {
            Files.walkFileTree(base, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, unsort ? vistor
                    : new Traveler(vistor));
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    private void add(String pattern) {
        // normalize pattern
        pattern = pattern.replace(File.separatorChar, '/')
                .replaceAll("\\*{3,}", "**")
                .replaceAll("\\*\\*([^/])", "**/*$1")
                .replaceAll("([^/])\\*\\*", "$1*/**")
                .replace("/$", "/**")
                .replace("^/", "")
                .replaceAll("\\*\\*/\\*\\*", "**");

        // separate pattern
        String[] patterns = pattern.split("/");

        // find all pattern for any
        int size = patterns.length - 1;

        if (size == 0) {
            // top level file only
        } else if (patterns[size].equals("**")) {
            // directory only
            direcories.add(new DirectoryPathMatcher(patterns));
        } else if (size == 1 && patterns[0].equals("**")) {
            // file only
            files.add(new FilePathMatcher(patterns[1]));
        } else {
            // both
        }
    }

    /**
     * @version 2010/12/19 12:09:00
     */
    private final class Traveler implements FileVisitor<Path> {

        private final FileVisitor<Path> delegator;

        /** The pattern matcher for file. */
        private final FilePathMatcher[] file;

        /** The pattern matcher for file. */
        private final DirectoryPathMatcher[] directory;

        private int depth = 0;

        private final boolean[] unconditional = new boolean[64];

        /**
         * @param delegator
         */
        private Traveler(FileVisitor<Path> delegator) {
            this.delegator = delegator;

            file = PathSet.this.files.toArray(new FilePathMatcher[PathSet.this.files.size()]);
            directory = PathSet.this.direcories.toArray(new DirectoryPathMatcher[PathSet.this.direcories.size()]);
        }

        /**
         * @see java.nio.file.FileVisitor#visitFile(java.lang.Object,
         *      java.nio.file.attribute.BasicFileAttributes)
         */
        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
            if (unconditional[depth]) {
                return delegator.visitFile(path, attrs);
            } else {
                String name = path.getName().toString();

                for (FilePathMatcher matcher : file) {
                    if (matcher.match(name)) {
                        return delegator.visitFile(path, attrs);
                    }
                }
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
        public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs) throws IOException {
            if (depth != 0) {
                if (unconditional[depth]) {
                    return delegator.preVisitDirectory(path, attrs);
                } else {
                    String name = path.getName().toString();

                    for (DirectoryPathMatcher matcher : directory) {
                        if (matcher.pattern.match(name)) {
                            unconditional[depth + 1] = true;
                            break;
                        }
                    }
                }
            }

            depth++;
            return CONTINUE;
        }

        /**
         * @see java.nio.file.FileVisitor#postVisitDirectory(java.lang.Object, java.io.IOException)
         */
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {

            if (depth != 0) {
                depth--;

                unconditional[depth + 1] = false;
            }
            return CONTINUE;
        }
    }

    /**
     * @version 2011/01/19 11:57:56
     */
    private static final class FilePathMatcher {

        /** The file name pattern. */
        private final Wildcard pattern;

        /**
         * @param pattern
         */
        private FilePathMatcher(String pattern) {
            this.pattern = new Wildcard(pattern);
        }

        /**
         * @param name
         * @return
         */
        public boolean match(String name) {
            return pattern.match(name);
        }
    }

    /**
     * @version 2011/01/19 11:59:50
     */
    private static final class DirectoryPathMatcher {

        /** The file name pattern. */
        private final Wildcard pattern;

        /**
         * @param pattern
         */
        private DirectoryPathMatcher(String[] patterns) {
            if (patterns.length != 2) {
                throw new IllegalArgumentException("You can specify the single directory pattern only.");
            }
            this.pattern = new Wildcard(patterns[0]);
        }
    }
}
