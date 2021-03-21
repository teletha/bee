/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.api;

import java.util.Arrays;
import java.util.List;

import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.artifact.JavaScopes;

public enum Scope {

    /** Depend at anytime. */
    Compile(JavaScopes.COMPILE, JavaScopes.PROVIDED, JavaScopes.SYSTEM),

    /** Depend at test phase only. */
    Test(JavaScopes.TEST),

    /** Depend at runtime phase only. */
    Runtime(JavaScopes.RUNTIME, JavaScopes.COMPILE),

    /** Depend at runtime phase only. */
    Provided(JavaScopes.PROVIDED),

    /** Depend at runtime phase only. */
    System(JavaScopes.SYSTEM),

    /** Depend at Pluggable Annotation Processing phase only. */
    Annotation("Pluggable Annotation Processing");

    /** The internal flag. */
    private final String type;

    /** The acceptable scope. */
    private List<String> acceptable;

    /**
     * Scope definition.
     * 
     * @param type A scope type.
     * @param acceptables A list of acceptable scope types.
     */
    private Scope(String type, String... acceptables) {
        this.type = type;
        this.acceptable = Arrays.asList(acceptables);
    }

    /**
     * Returns dependency filter.
     * 
     * @return
     */
    public DependencyFilter getFilter() {
        return (node, parents) -> {
            return accept(node.getDependency()) && parents.stream().map(DependencyNode::getDependency).allMatch(this::accept);
        };
    }

    /**
     * Test scope.
     * 
     * @param dependency
     * @return
     */
    public boolean accept(Dependency dependency) {
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
     * Find scope by keyword.
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

        if (keyword.equalsIgnoreCase("Pluggable Annotation Processing")) {
            return Annotation;
        }
        return Runtime;
    }
}