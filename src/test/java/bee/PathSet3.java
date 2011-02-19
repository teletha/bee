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

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

import ezbean.I;

/**
 * @version 2011/02/19 14:01:36
 */
public class PathSet3 extends PathSet {

    private ArrayList<Wildcard> includes = new ArrayList();

    private ArrayList<Wildcard> excludes = new ArrayList();

    private FileSystem system = FileSystems.getDefault();

    /**
     * @param base
     */
    public PathSet3(Path base) {
        super(base);
    }

    /**
     * @param base
     */
    public PathSet3(String base) {
        super(base);
    }

    /**
     * @see bee.PathSet#include(java.lang.String[])
     */
    @Override
    public PathSet include(String... patterns) {
        for (String pattern : patterns) {
            includes.add(new Wildcard(pattern));
        }
        return this;
    }

    /**
     * @see bee.PathSet#exclude(java.lang.String[])
     */
    @Override
    public PathSet exclude(String... patterns) {
        for (String pattern : patterns) {
            excludes.add(new Wildcard(pattern));
        }
        return this;
    }

    /**
     * @see bee.PathSet#scan(java.nio.file.FileVisitor)
     */
    @Override
    public void scan(FileVisitor<Path> vistor) {
        try {
            Files.walkFileTree(base, new Delegater(vistor, includes, excludes));
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * @see bee.PathSet#reset()
     */
    @Override
    public PathSet reset() {
        includes.clear();
        excludes.clear();
        return super.reset();
    }

    /**
     * @version 2011/02/19 14:06:00
     */
    private static class Delegater extends SimpleFileVisitor<Path> {

        private final FileVisitor<Path> delegeter;

        private final Wildcard[] includes;

        private final Wildcard[] excludes;

        private final boolean i;

        /**
         * @param delegeter
         */
        public Delegater(FileVisitor<Path> delegeter, ArrayList<Wildcard> includes, ArrayList<Wildcard> excludes) {
            this.delegeter = delegeter;
            this.includes = includes.toArray(new Wildcard[includes.size()]);
            this.excludes = excludes.toArray(new Wildcard[excludes.size()]);
            this.i = this.includes.length != 0;
        }

        /**
         * @see java.nio.file.SimpleFileVisitor#visitFile(java.lang.Object,
         *      java.nio.file.attribute.BasicFileAttributes)
         */
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            String name = file.toString();
            for (Wildcard matcher : excludes) {
                if (matcher.match(name)) {
                    return FileVisitResult.CONTINUE;
                }
            }

            for (Wildcard matcher : includes) {
                if (matcher.match(name)) {
                    return delegeter.visitFile(file, attrs);
                }
            }

            return i ? FileVisitResult.CONTINUE : delegeter.visitFile(file, attrs);

        }
    }
}
