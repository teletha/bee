/*
 * Copyright (C) 2018 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.util;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import filer.Filer;

/**
 * @version 2012/03/26 15:44:18
 */
public class PathSet implements Iterable<PathPattern> {

    /** The path pattern set. */
    private final List<PathPattern> set = new ArrayList();

    /**
     * <p>
     * Empty path set.
     * </p>
     */
    public PathSet() {
    }

    /**
     * <p>
     * Path set with the specified paths.
     * </p>
     * 
     * @param paths
     */
    public PathSet(Path... paths) {
        for (Path path : paths) {
            add(path);
        }
    }

    /**
     * <p>
     * Path set with the specified paths.
     * </p>
     * 
     * @param paths
     */
    public PathSet(Collection<Path> paths) {
        for (Path path : paths) {
            add(path);
        }
    }

    /**
     * <p>
     * Path set with the specified paths.
     * </p>
     * 
     * @param paths
     */
    public PathSet(PathPattern path) {
        set.add(path);
    }

    /**
     * <p>
     * Add path patterns.
     * </p>
     * 
     * @param base
     * @param patterns
     */
    public void add(Path base, String... patterns) {
        set.add(new PathPattern(base, patterns));
    }

    /**
     * <p>
     * Copy all files to the specifed path.
     * </p>
     * 
     * @param destination
     */
    public void copyTo(Path destination, String... patterns) {
        Paths.createDirectory(destination);

        for (PathPattern pattern : set) {
            Filer.copy(pattern.base, destination, pattern.mix(patterns));
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
        for (PathPattern pattern : set) {
            Filer.move(pattern.base, destination, pattern.mix(patterns));
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
        for (PathPattern pattern : set) {
            Filer.delete(pattern.base, pattern.mix(patterns));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<PathPattern> iterator() {
        return set.iterator();
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

        for (PathPattern pattern : set) {
            paths.addAll(Filer.walk(pattern.base, pattern.mix()).toList());
        }
        return paths;
    }
}
