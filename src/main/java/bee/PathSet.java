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
    private Set<Wildcard> excludeDirectory = new CopyOnWriteArraySet();

    /** The actual filter set for directory only matcher. */
    private Set<Wildcard> excludeFile = new CopyOnWriteArraySet();

    /** The actual filter set for file only matcher. */
    private Set<Wildcard> includers = new CopyOnWriteArraySet();

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
        for (String pattern : patterns) {
            includers.add(new Wildcard(pattern));
        }
        return this;
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
        for (String pattern : patterns) {
            String[] parsed = pattern.replace(File.separatorChar, '/')
                    .replaceAll("\\*{3,}", "**")
                    .replaceAll("\\*\\*([^/])", "**/*$1")
                    .replaceAll("([^/])\\*\\*", "$1*/**")
                    .replace("/$", "/**")
                    .replace("^/", "")
                    .replaceAll("\\*\\*/\\*\\*", "**")
                    .split("/");

            if (parsed.length == 2) {
                if (parsed[0].equals("**")) {
                    excludeFile.add(new Wildcard(parsed[1]));
                } else if (parsed[1].equals("**")) {
                    excludeDirectory.add(new Wildcard(parsed[0]));
                }
            }
        }
        return this;
    }

    /**
     * <p>
     * Exclude the default
     * </p>
     * 
     * @param patterns A exclude directory patterns.
     * @return {@link PathSet} instance to chain API.
     */
    public PathSet excludeDefault(boolean exclusion) {
        if (exclusion) {
            exclude("**/.*", ".*/**", "CVS/**", "SCCS/**");
        }
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
        boolean unsort = includers.isEmpty() && excludeDirectory.isEmpty() && excludeFile.isEmpty();

        try {
            Files.walkFileTree(base, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, unsort ? vistor
                    : new Traveler(vistor, excludeDirectory, excludeFile, includers));
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * @version 2011/02/15 15:49:01
     */
    private final class Traveler implements FileVisitor<Path> {

        /** The simple directory exclude patterns. */
        private final Wildcard[] excludeDirectory;

        /** The simple file exclude patterns. */
        private final Wildcard[] excludeFile;

        /** We should exclude something? */
        private final boolean exclude;

        /** The simple file include patterns. */
        private final Wildcard[] includeFile;

        /** We should include some files? */
        private final boolean include;

        /** The actual file visitor. */
        private final FileVisitor<Path> delegator;

        /**
         * @param delegator
         */
        private Traveler(FileVisitor<Path> delegator, Set<Wildcard> excludeDirectory, Set<Wildcard> excludeFile, Set<Wildcard> includers) {
            this.delegator = delegator;
            this.excludeDirectory = excludeDirectory.toArray(new Wildcard[excludeDirectory.size()]);
            this.excludeFile = excludeFile.toArray(new Wildcard[excludeFile.size()]);
            this.includeFile = includers.toArray(new Wildcard[includers.size()]);
            this.exclude = this.excludeDirectory.length != 0;
            this.include = this.includeFile.length != 0;
        }

        /**
         * @see java.nio.file.FileVisitor#visitFile(java.lang.Object,
         *      java.nio.file.attribute.BasicFileAttributes)
         */
        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
            String name = path.getName().toString();

            // Exclude has high priority.
            for (Wildcard wildcard : excludeFile) {
                if (wildcard.match(name)) {
                    return CONTINUE;
                }
            }

            if (include) {
                for (Wildcard wildcard : includeFile) {
                    if (wildcard.match(name)) {
                        return delegator.visitFile(path, attrs);
                    }
                }
                return CONTINUE;
            } else {
                return delegator.visitFile(path, attrs);
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
            if (exclude) {
                String name = path.getName().toString();

                for (Wildcard wildcard : excludeDirectory) {
                    if (wildcard.match(name)) {
                        return SKIP_SUBTREE;
                    }
                }
            }

            // delegate
            return delegator.preVisitDirectory(path, attrs);
        }

        /**
         * @see java.nio.file.FileVisitor#postVisitDirectory(java.lang.Object, java.io.IOException)
         */
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            return CONTINUE;
        }
    }
}
