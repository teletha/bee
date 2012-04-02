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

import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import kiss.I;

/**
 * @version 2012/03/26 15:44:18
 */
public class PathSet implements Iterable<Path> {

    /** The path pattern set. */
    private final List<Pattern> set = new ArrayList();

    /**
     * <p>
     * Add path patterns.
     * </p>
     * 
     * @param base
     * @param patterns
     */
    public void add(Path base, String... patterns) {
        set.add(new Pattern(base, patterns));
    }

    /**
     * <p>
     * Copy all files to the specifed path.
     * </p>
     * 
     * @param destination
     */
    public void copyTo(Path destination, String... patterns) {
        for (Pattern pattern : set) {
            I.copy(pattern.base, destination, pattern.mix(patterns));
        }
    }

    /**
     * <p>
     * Move all files to the specifed path.
     * </p>
     * 
     * @param destination
     */
    public void moveTo(Path destination, String... patterns) {
        for (Pattern pattern : set) {
            I.move(pattern.base, destination, pattern.mix(patterns));
        }
    }

    /**
     * <p>
     * Move all files to the specifed path.
     * </p>
     * 
     * @param destination
     */
    public void delete(String... patterns) {
        for (Pattern pattern : set) {
            I.delete(pattern.base, pattern.mix(patterns));
        }
    }

    /**
     * <p>
     * Walk all files and directories.
     * </p>
     * 
     * @param visitor
     */
    public void each(FileVisitor<Path> visitor) {
        for (Pattern pattern : set) {
            I.walk(pattern.base, visitor, pattern.mix());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Path> iterator() {
        List<Path> paths = new ArrayList();

        for (Pattern pattern : set) {
            paths.add(pattern.base);
        }
        return paths.iterator();
    }

    /**
     * <p>
     * Walk all root directories.
     * </p>
     * 
     * @return
     */
    public List<Path> getFiles() {
        List<Path> paths = new ArrayList();

        for (Pattern pattern : set) {
            paths.addAll(I.walk(pattern.base, pattern.mix()));
        }
        return paths;
    }

    /**
     * @version 2012/03/26 15:45:26
     */
    private static final class Pattern {

        /** The base path. */
        private final Path base;

        /** The include/exclude patterns. */
        private final List<String> patterns = new ArrayList();

        /**
         * @param base
         * @param patterns
         */
        private Pattern(Path base, String[] patterns) {
            this.base = base;

            for (String pattern : patterns) {
                this.patterns.add(pattern);
            }
        }

        /**
         * <p>
         * Helper method to mix addtional patterns.
         * </p>
         * 
         * @param additions
         * @return
         */
        private String[] mix(String... additions) {
            List<String> patterns = new ArrayList(this.patterns);

            for (String addition : additions) {
                patterns.add(addition);
            }
            return patterns.toArray(new String[patterns.size()]);
        }
    }
}
