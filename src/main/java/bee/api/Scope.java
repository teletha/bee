/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.api;

import org.sonatype.aether.graph.DependencyFilter;
import org.sonatype.aether.util.artifact.JavaScopes;
import org.sonatype.aether.util.filter.DependencyFilterUtils;

/**
 * @version 2012/03/21 20:20:37
 */
public enum Scope {

    /** Depend at anytime. */
    Compile(JavaScopes.COMPILE),

    /** Depend at test phase only. */
    Test(JavaScopes.TEST),

    /** Depend at runtime phase only. */
    Runtime(JavaScopes.RUNTIME),

    /** Depend at runtime phase only. */
    Provided(JavaScopes.PROVIDED),

    /** Depend at runtime phase only. */
    System(JavaScopes.SYSTEM);

    /** The internal flag. */
    private final String type;

    /**
     * <p>
     * Scope definition.
     * </p>
     * 
     * @param type
     */
    private Scope(String type) {
        this.type = type;
    }

    /**
     * <p>
     * Returns dependency filter.
     * </p>
     * 
     * @return
     */
    public DependencyFilter getFilter() {
        return DependencyFilterUtils.classpathFilter(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return type;
    }

    /**
     * <p>
     * Find scope by keyword.
     * </p>
     * 
     * @param keyword
     * @return
     */
    public static Scope by(String keyword) {
        if (keyword == null || keyword.length() == 0 || keyword.equalsIgnoreCase("compile")) {
            return Compile;
        }

        if (keyword.equalsIgnoreCase("test")) {
            return Test;
        }

        if (keyword.equalsIgnoreCase("provided")) {
            return Provided;
        }

        if (keyword.equalsIgnoreCase("system")) {
            return System;
        }
        return Runtime;
    }
}
