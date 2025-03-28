/*
 * Copyright (C) 2025 The BEE Development Team
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

public enum Scope {

    /** Depend at anytime. */
    Compile("compile", "provided", "system"),

    /** Depend at test phase only. */
    Test("test"),

    /** Depend at runtime phase only. */
    Runtime("runtime", "compile"),

    /** Depend at runtime phase only. */
    Provided("provided"),

    /** Depend at runtime phase only. */
    System("system"),

    /** Depend at Pluggable Annotation Processing phase only. */
    Annotation("annotation");

    /** The identifier. */
    public final String id;

    /** The acceptable scope. */
    private List<String> acceptable;

    /**
     * Scope definition.
     * 
     * @param id A scope identifier.
     * @param acceptables A list of acceptable scope types.
     */
    private Scope(String id, String... acceptables) {
        this.id = id;
        this.acceptable = Arrays.asList(acceptables);
    }

    /**
     * Test scope.
     * 
     * @param scope
     * @return
     */
    public boolean accept(String scope) {
        return scope == null || id.equals(scope) || acceptable.contains(scope);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return id;
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