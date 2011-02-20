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
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;

import ezbean.I;

/**
 * @version 2011/02/15 15:51:49
 */
public class PathSet implements Iterable<Path> {

    /** The base path. */
    protected final Path base;

    /** The actual filter set for directory only matcher. */
    private Set<Wildcard> includeFile = new CopyOnWriteArraySet();

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
        return parse(patterns, includeFile);
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
        return parse(patterns, excludeFile);
    }

    /**
     * <p>
     * Helper method to parse the specified patterns and store it for the suitable collection.
     * </p>
     */
    private PathSet parse(String[] patterns, Set<Wildcard> forFile) {
        for (String pattern : patterns) {
            pattern = pattern.replace(File.separatorChar, '/')
                    .replaceAll("\\*{2,}", "*")
                    .replace("/$", "/*")
                    .replace("^/", "")
                    .replaceAll("\\*/\\*", "*")
                    .replace('/', File.separatorChar);

            System.out.println(pattern);
            forFile.add(new Wildcard(pattern));

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

    /**
     * <p>
     * Copy all file in this {@link PathSet} to the specified path.
     * </p>
     */
    public boolean copyTo(Path dist) {
        Copy copy = new Copy(base, dist);

        // Scan file system
        scan(copy);

        // API definition
        return copy.success;
    }

    /**
     * @version 2011/02/16 12:19:34
     */
    private static final class Copy extends SimpleFileVisitor<Path> {

        /** The source location. */
        private final Path from;

        /** The target location. */
        private final Path to;

        /** The sccess flag. */
        private boolean success = true;

        /**
         * @param from
         * @param to
         */
        private Copy(Path from, Path to) {
            this.from = from;
            this.to = to;
        }

        /**
         * @see java.nio.file.SimpleFileVisitor#preVisitDirectory(java.lang.Object,
         *      java.nio.file.attribute.BasicFileAttributes)
         */
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            // System.out.println(dir + "   " + to.resolve(from.relativize(dir)));
            Path target = to.resolve(from.relativize(dir));

            if (target.notExists()) {
                target.createDirectory();
            }
            return CONTINUE;
        }

        /**
         * @see java.nio.file.SimpleFileVisitor#visitFile(java.lang.Object,
         *      java.nio.file.attribute.BasicFileAttributes)
         */
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Path target = to.resolve(from.relativize(file));
            file.copyTo(target);
            return CONTINUE;
        }
    }

    public void moveTo(Path dist) {
        copyTo(dist);
        delete();
    }

    /**
     * <p>
     * Delete all file in this {@link PathSet}.
     * </p>
     */
    public boolean delete() {
        Delete delete = new Delete();

        // Scan file system
        scan(delete);

        // API definition
        return delete.success;
    }

    /**
     * @version 2011/02/16 11:55:42
     */
    private static final class Delete extends SimpleFileVisitor<Path> {

        /** The sccess flag. */
        private boolean success = true;

        /**
         * @see java.nio.file.SimpleFileVisitor#postVisitDirectory(java.lang.Object,
         *      java.io.IOException)
         */
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            try {
                dir.delete();
            } catch (Exception e) {
                success = false;
            }
            return CONTINUE;
        }

        /**
         * @see java.nio.file.SimpleFileVisitor#visitFile(java.lang.Object,
         *      java.nio.file.attribute.BasicFileAttributes)
         */
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            try {
                file.delete();
            } catch (Exception e) {
                success = false;
            }
            return CONTINUE;
        }
    }

    /**
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<Path> iterator() {
        Counter counter = new Counter();
        // scan(counter);

        Executors.newSingleThreadExecutor().execute(counter);

        return counter;
    }

    /**
     * @version 2011/02/18 16:17:29
     */
    private class Counter extends SimpleFileVisitor<Path> implements Iterator<Path>, Runnable {

        /** The pass point. */
        private ArrayBlockingQueue<Path> queue = new ArrayBlockingQueue(20);

        /** The next element. */
        private Path next;

        /** The flag for termination. */
        private boolean finish = false;

        private Thread thread = Thread.currentThread();

        /**
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            scan(this);

            finish = true;

            thread.interrupt();
        }

        /**
         * @see java.util.Iterator#hasNext()
         */
        @Override
        public boolean hasNext() {
            if (finish) {
                if (queue.isEmpty()) {
                    return false;
                } else {
                    next = queue.poll();

                    return true;
                }
            } else {
                if (queue.isEmpty()) {
                    try {
                        next = queue.take();

                        return true;
                    } catch (InterruptedException e) {
                        return false;
                    }
                } else {
                    next = queue.poll();

                    return true;
                }
            }
        }

        /**
         * @see java.util.Iterator#next()
         */
        @Override
        public Path next() {
            return next;
        }

        /**
         * @see java.util.Iterator#remove()
         */
        @Override
        public void remove() {
        }

        /**
         * @see java.nio.file.SimpleFileVisitor#visitFile(java.lang.Object,
         *      java.nio.file.attribute.BasicFileAttributes)
         */
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            queue.add(file);
            return CONTINUE;
        }
    }

    /**
     * <p>
     * Scan the file system from the specified base directory and its children. If you specify the
     * include/exclude patterns, this method recognize it.
     * </p>
     * 
     * @param vistor A file visitor that all accepted files and directories are passed.
     */
    public void scan(FileVisitor<Path> vistor) {
        try {
            Files.walkFileTree(base, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new Traveler(vistor, base, excludeFile, includeFile));
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * Reset the current settings except for base directory.
     * </p>
     * 
     * @return {@link PathSet} instance to chain API.
     */
    public PathSet reset() {
        includeFile.clear();
        excludeFile.clear();
        excludeDirectory.clear();

        // API chain
        return this;
    }

    /**
     * @version 2011/02/15 15:49:01
     */
    private static final class Traveler implements FileVisitor<Path> {

        /** The simple file include patterns. */
        private final Wildcard[] includeFile;

        /** We should exclude something? */
        private final int includeFileSize;

        /** The simple file exclude patterns. */
        private final Wildcard[] excludeFile;

        /** We should exclude something? */
        private final int excludeFileSize;

        /** The actual file visitor. */
        private final FileVisitor<Path> delegator;

        /** The prefix length for the base path. */
        private final int prefix;

        /** The current depth of file system. */
        private int depth = 0;

        /** The last depth that we should include all files. */
        private int depthForDirectoryIncluding = Integer.MAX_VALUE;

        /**
         * @param delegator
         */
        private Traveler(FileVisitor<Path> delegator, Path base, Set<Wildcard> excludeFile, Set<Wildcard> includeFile) {
            this.delegator = delegator;
            this.prefix = base.toString().length() + 1;
            this.includeFile = includeFile.toArray(new Wildcard[includeFile.size()]);
            this.excludeFile = excludeFile.toArray(new Wildcard[excludeFile.size()]);

            includeFileSize = this.includeFile.length;
            excludeFileSize = this.excludeFile.length;
        }

        /**
         * @see java.nio.file.FileVisitor#visitFile(java.lang.Object,
         *      java.nio.file.attribute.BasicFileAttributes)
         */
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            String name = file.toString().substring(prefix);

            for (Wildcard matcher : excludeFile) {
                if (matcher.match(name)) {
                    return CONTINUE;
                }
            }

            for (Wildcard matcher : includeFile) {
                if (matcher.match(name)) {
                    return delegator.visitFile(file, attrs);
                }
            }
            return includeFileSize == 0 ? delegator.visitFile(file, attrs) : CONTINUE;
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
            return CONTINUE;
        }

        /**
         * @see java.nio.file.FileVisitor#postVisitDirectory(java.lang.Object, java.io.IOException)
         */
        @Override
        public FileVisitResult postVisitDirectory(Path directory, IOException exc) throws IOException {
            return CONTINUE;
        }
    }
}
