/*
 * Copyright (C) 2015 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.api;

import java.util.Arrays;
import java.util.List;

import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.artifact.JavaScopes;

/**
 * @version 2016/06/30 11:14:45
 */
public enum Scope {

    /** Depend at anytime. */
    Compile(JavaScopes.COMPILE, JavaScopes.PROVIDED, JavaScopes.SYSTEM),

    /** Depend at test phase only. */
    Test(JavaScopes.TEST, JavaScopes.COMPILE, JavaScopes.PROVIDED, JavaScopes.RUNTIME, JavaScopes.SYSTEM),

    /** Depend at runtime phase only. */
    Runtime(JavaScopes.RUNTIME, JavaScopes.COMPILE),

    /** Depend at runtime phase only. */
    Provided(JavaScopes.PROVIDED),

    /** Depend at runtime phase only. */
    System(JavaScopes.SYSTEM);

    /** The internal flag. */
    private final String type;

    /** The acceptable scope. */
    private List<String> acceptable;

    /**
     * <p>
     * Scope definition.
     * </p>
     * 
     * @param type A scope type.
     * @param acceptables A list of acceptable scope types.
     */
    private Scope(String type, String... acceptables) {
        this.type = type;
        this.acceptable = Arrays.asList(acceptables);
    }

    /**
     * <p>
     * Returns dependency filter.
     * </p>
     * 
     * @return
     */
    public DependencyFilter getFilter() {
        return (node, parents) -> {
            return accept(node) && parents.stream().allMatch(this::accept);
        };
    }

    /**
     * <p>
     * Test scope.
     * </p>
     * 
     * @param dependency
     * @return
     */
    private boolean accept(DependencyNode node) {
        Dependency dependency = node.getDependency();

        if (dependency == null) {
            return true;
        }

        String scope = dependency.getScope();

        return type.equals(scope) || acceptable.contains(scope);
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
