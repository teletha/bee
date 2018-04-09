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
import java.util.Arrays;
import java.util.List;

import filer.Filer;

/**
 * @version 2012/03/26 15:45:26
 */
public final class PathPattern {

    /** The base path. */
    public final Path base;

    /** The include/exclude patterns. */
    public final List<String> patterns;

    /**
     * @param base
     * @param patterns
     */
    public PathPattern(Path base, String... patterns) {
        this.base = base;
        this.patterns = Arrays.asList(patterns);
    }

    /**
     * <p>
     * Helper method to mix addtional patterns.
     * </p>
     * 
     * @param additions
     * @return
     */
    String[] mix(String... additions) {
        List<String> patterns = new ArrayList(this.patterns);

        for (String addition : additions) {
            patterns.add(addition);
        }
        return patterns.toArray(new String[patterns.size()]);
    }

    /**
     * List up all paths.
     * 
     * @param patterns
     * @return
     */
    public List<Path> list(String... patterns) {
        return Filer.walk(base, mix(patterns)).toList();
    }
}
