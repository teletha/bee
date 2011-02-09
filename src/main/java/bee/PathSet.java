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
    private Set<Wildcard> files = new CopyOnWriteArraySet();

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
            files.add(new Wildcard(patterns[1]));
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
        private final Wildcard[] file;

        /** The pattern matcher for file. */
        private final DirectoryPathMatcher[] directory;

        private int depth = 0;

        private final boolean[] unconditional = new boolean[64];

        private final int[][] steps;

        private int currentDepth = -1;

        private int unconditionalDepth = 1000;

        private int length = 0;

        /**
         * @param delegator
         */
        private Traveler(FileVisitor<Path> delegator) {
            this.delegator = delegator;

            file = PathSet.this.files.toArray(new Wildcard[PathSet.this.files.size()]);
            directory = PathSet.this.direcories.toArray(new DirectoryPathMatcher[PathSet.this.direcories.size()]);

            steps = new int[directory.length][64];
            length = directory.length;
        }

        /**
         * @see java.nio.file.FileVisitor#visitFile(java.lang.Object,
         *      java.nio.file.attribute.BasicFileAttributes)
         */
        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
            if (unconditionalDepth <= currentDepth) {
                return delegator.visitFile(path, attrs);
            } else {
                String name = path.getName().toString();

                for (Wildcard wildcard : file) {
                    if (wildcard.match(name)) {
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
            currentDepth++;

            if (unconditionalDepth < currentDepth) {
                delegator.preVisitDirectory(path, attrs);
            } else {
                String name = path.getName().toString();
                System.out.println(path);
                for (int i = 0; i < length; i++) {
                    switch (directory[i].enter(currentDepth, name)) {
                    case 1: // unconditionaly match
                        length = i + 1;
                        System.out.println("set unconditional " + currentDepth);
                        unconditionalDepth = currentDepth;
                        break;

                    default:
                        break;
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
            if (unconditionalDepth < currentDepth) {
                delegator.postVisitDirectory(dir, exc);
            } else if (unconditionalDepth == currentDepth) {
                unconditionalDepth = 1000; // reset

                for (int i = 0; i < length; i++) {
                    directory[i].exit(currentDepth);
                }
                delegator.postVisitDirectory(dir, exc);

                length = directory.length;
            } else {
                for (int i = 0; i < length; i++) {
                    directory[i].exit(currentDepth);
                }
                delegator.postVisitDirectory(dir, exc);
            }

            currentDepth--;
            return CONTINUE;
        }
    }

    /**
     * @version 2011/01/19 11:59:50
     */
    private static final class DirectoryPathMatcher {

        /** The file name pattern. */
        private final Wildcard pattern;

        private Wildcard[] wildcards;

        private int current = 0;

        private int[] steps = new int[64];

        /**
         * @param pattern
         */
        private DirectoryPathMatcher(String[] patterns) {

            this.pattern = new Wildcard(patterns[0]);
            this.wildcards = new Wildcard[patterns.length];

            for (int i = 0; i < patterns.length; i++) {
                wildcards[i] = patterns[i].equals("**") ? null : new Wildcard(patterns[i]);
            }

        }

        private int enter(int depth, String name) {
            steps[depth] = current;

            Wildcard wildcard = wildcards[current];

            if (wildcard == null) {
                // The current is directory wildcard pattern, so next pattern must be existed and
                // not null.
                System.out.println("enter wild   " + current + "   " + wildcards[current + 1].match(name));
                wildcard = wildcards[current + 1];

                if (wildcard.match(name)) {
                    // Step into next pattern.
                    current += 2;

                    if (current == wildcards.length - 1) {
                        // Make unconditionaly match
                        return 1;
                    }
                } else {
                    // Try subdirectory.
                }
            } else {
                // The current is direct-child-directory pattern.
                System.out.println("enter normal   " + current + "   " + wildcard.match(name));

                if (wildcard.match(name)) {
                    // Step int next pattern.
                    current += 1;

                    if (current == wildcards.length - 1) {
                        // Make unconditionaly match.
                        return 1;
                    }
                } else {
                    // Reset current pattern.
                    System.out.println("reset " + steps[depth]);
                    current = steps[depth];
                }
            }

            return 0;
        }

        private void exit(int depth) {
            System.out.println("exit  " + current + "  " + steps[depth]);
            current = steps[depth];
            steps[depth] = 0;
        }
    }
}
