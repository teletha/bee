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

    /** The actual filter set for file only matcher. */
    private Set<BothPathMatcher> boths = new CopyOnWriteArraySet();

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
            boths.add(new BothPathMatcher(patterns));
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

        private int depth = -1;

        private int unconditional = 1000;

        private int length = 0;

        /**
         * @param delegator
         */
        private Traveler(FileVisitor<Path> delegator) {
            this.delegator = delegator;

            file = PathSet.this.files.toArray(new Wildcard[PathSet.this.files.size()]);
            directory = PathSet.this.direcories.toArray(new DirectoryPathMatcher[PathSet.this.direcories.size()]);

            length = directory.length;
        }

        /**
         * @see java.nio.file.FileVisitor#visitFile(java.lang.Object,
         *      java.nio.file.attribute.BasicFileAttributes)
         */
        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
            if (unconditional <= depth) {
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
            depth++;

            if (unconditional < depth) {
                return delegator.preVisitDirectory(path, attrs); // unconditionaly
            }

            String name = path.getName().toString();

            for (int i = 0; i < length; i++) {
                switch (directory[i].enter(depth, name)) {
                case 1: // unconditionaly match
                    length = i + 1;
                    System.out.println("set unconditional " + depth);
                    unconditional = depth;
                    break;

                default:
                    break;
                }
            }

            return CONTINUE;
        }

        /**
         * @see java.nio.file.FileVisitor#postVisitDirectory(java.lang.Object, java.io.IOException)
         */
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            if (unconditional < depth) {
                delegator.postVisitDirectory(dir, exc);
            } else if (unconditional == depth) {
                unconditional = 1000; // reset

                for (int i = 0; i < length; i++) {
                    directory[i].exit(depth);
                }
                delegator.postVisitDirectory(dir, exc);

                length = directory.length;
            } else {
                for (int i = 0; i < length; i++) {
                    directory[i].exit(depth);
                }
                delegator.postVisitDirectory(dir, exc);
            }

            depth--;
            return CONTINUE;
        }
    }

    /**
     * @version 2011/01/19 11:59:50
     */
    private static final class DirectoryPathMatcher {

        private Wildcard[] wildcards;

        private int current = 0;

        private int[] steps = new int[64];

        private final boolean root;

        /**
         * @param pattern
         */
        private DirectoryPathMatcher(String[] patterns) {
            this.wildcards = new Wildcard[patterns.length];

            for (int i = 0; i < patterns.length; i++) {
                wildcards[i] = patterns[i].equals("**") ? null : new Wildcard(patterns[i]);
            }
            this.root = wildcards[0] != null;
        }

        private int enter(int depth, String name) {
            if (root && current == 0 && depth != 1) {
                return 0;
            }

            steps[depth] = current;

            Wildcard wildcard = wildcards[current];

            if (wildcard == null) {
                // The current is directory wildcard pattern, so next pattern must be existed and
                // not null.
                System.out.println("enter wild   " + current + "   " + wildcards[current + 1].match(name) + "   " + name);
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
                System.out.println("enter normal   " + current + "   " + wildcard.match(name) + "  " + name);

                if (wildcard.match(name)) {
                    // Step int next pattern.
                    current += 1;

                    if (current == wildcards.length - 1) {
                        // Make unconditionaly match.
                        return 1;
                    }
                } else {
                    // Reset current pattern.
                    int back = depth;

                    if (depth != 0) {
                        back--;

                        current = steps[back];
                        System.out.println(root + "   " + depth + "  " + current);
                        return enter(back, name);
                    }

                    System.out.println("reset " + steps[back]);
                    current = steps[back];
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

    /**
     * @version 2011/01/19 11:59:50
     */
    private static final class BothPathMatcher {

        private Wildcard[] wildcards;

        private int current = 0;

        private int[] steps = new int[64];

        private final boolean root;

        /**
         * @param pattern
         */
        private BothPathMatcher(String[] patterns) {
            this.wildcards = new Wildcard[patterns.length];

            for (int i = 0; i < patterns.length; i++) {
                wildcards[i] = patterns[i].equals("**") ? null : new Wildcard(patterns[i]);
            }
            this.root = wildcards[0] != null;
        }

        private int enter(int depth, String name) {
            if (root && current == 0 && depth != 1) {
                return 0;
            }

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
                    int back = depth;

                    if (depth != 0) {
                        back--;

                        current = steps[back];

                        return enter(back, name);
                    }

                    System.out.println("reset " + steps[back]);
                    current = steps[back];
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
