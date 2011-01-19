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
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.ArrayList;
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

    /** The actual filter set. */
    private Set<Pattern> filters = new CopyOnWriteArraySet();

    /** The actual filter set for file only matcher. */
    private Set<PatternMatch> files = new CopyOnWriteArraySet();

    private FilenameFilter filter;

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
            filters.add(new Pattern(pattern));
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
        try {
            Files.walkFileTree(base, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new Traveler(vistor));
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
        private final FilePathMatcher[] files;

        /** The pattern matcher for directory. */
        private final ArrayList<DirectoryPathMatcher> directories = new ArrayList();

        private final ArrayList<Pattern> current;

        private final ArrayList<Pattern> remover = new ArrayList();

        private final ArrayDeque<ArrayList<Pattern>> useless = new ArrayDeque();

        private int depth = 0;

        /**
         * @param delegator
         */
        private Traveler(FileVisitor<Path> delegator) {
            this.delegator = delegator;

            // copy
            current = new ArrayList(filters.size());
            current.addAll(filters);

            files = PathSet.this.files.toArray(new FilePathMatcher[0]);
        }

        /**
         * @see java.nio.file.FileVisitor#preVisitDirectory(java.lang.Object,
         *      java.nio.file.attribute.BasicFileAttributes)
         */
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (depth != 0) {
                String name = dir.getName().toString();

                for (Pattern pattern : current) {
                    if (pattern.use) {
                        if (pattern.matchRoute(name)) {
                            depth++;
                            return CONTINUE;
                        }
                    }
                }
                return CONTINUE;
            }

            depth++;
            return CONTINUE;
        }

        /**
         * @see java.nio.file.FileVisitor#visitFile(java.lang.Object,
         *      java.nio.file.attribute.BasicFileAttributes)
         */
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            int size = files.length;

            if (size == 0) {
                return delegator.visitFile(file, attrs);
            } else {
                String name = file.getName().toString();

                for (FilePathMatcher matcher : files) {
                    if (matcher.match(name)) {
                        delegator.visitFile(file, attrs);
                        break;
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
         * @see java.nio.file.FileVisitor#postVisitDirectory(java.lang.Object, java.io.IOException)
         */
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            depth--;

            if (depth != 0) {
                // System.out.println(dir);
            }
            return CONTINUE;
        }
    }

    /**
     * @version 2010/12/20 23:52:12
     */
    private static class Pattern {

        /** The normalized pattern for output. */
        private final String pattern;

        /** The pattern for directory route. */
        private final Wildcard[] directoryPattern;

        /** The pattern for file name. */
        private final Wildcard filePattern;

        private int depth = 0;

        private boolean use = false;

        /**
         * @param pattern
         */
        private Pattern(String pattern) {
            // normalize pattern
            this.pattern = pattern.replace(File.separatorChar, '/')
                    .replaceAll("\\*{3,}", "**")
                    .replaceAll("\\*\\*([^/])", "**/*$1")
                    .replaceAll("([^/])\\*\\*", "$1*/**")
                    .replace("/$", "/**")
                    .replace("^/", "");

            // separate pattern
            String[] patterns = this.pattern.split("/");

            // find all pattern for any
            for (int i = 0; i < patterns.length; i++) {
                if (patterns[i].equals("**")) {
                    patterns[i] = null;
                }
            }

            // analyze pattern
            directoryPattern = new Wildcard[patterns.length - 1];
            filePattern = new Wildcard(patterns[directoryPattern.length]);

            for (int i = 0; i < directoryPattern.length; i++) {
                directoryPattern[i] = new Wildcard(patterns[i]);
            }
        }

        private boolean matchRoute(String name) {
            return true;
        }

        private boolean matchName(String name) {
            return filePattern.match(name);
        }

        private boolean isUseless() {
            return false;
        }
    }

    /**
     * @version 2011/01/19 11:57:54
     */
    private static interface PatternMatch {

        boolean match(String name);
    }

    /**
     * @version 2011/01/19 11:57:56
     */
    private static final class FilePathMatcher implements PatternMatch {

        /** The file name pattern. */
        private final Wildcard pattern;

        /**
         * @param pattern
         */
        private FilePathMatcher(String pattern) {
            this.pattern = new Wildcard(pattern);
        }

        /**
         * @see bee.PathSet.PatternMatch#match(java.lang.String)
         */
        @Override
        public boolean match(String name) {
            return pattern.match(name);
        }
    }

    /**
     * @version 2011/01/19 11:59:50
     */
    private static final class DirectoryPathMatcher implements PatternMatch {

        /**
         * @see bee.PathSet.PatternMatch#match(java.lang.String)
         */
        @Override
        public boolean match(String name) {
            return false;
        }

    }
}
