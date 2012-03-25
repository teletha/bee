/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.definition;

/**
 * @version 2012/03/21 20:20:37
 */
public enum Scope {

    /** Depend at anytime. */
    Compile(1 | 2 | 4 | 8),

    /** Depend at test phase only. */
    Test(1 | 2),

    /** Depend at runtime phase only. */
    Runtime(4),

    /** Depend at runtime phase only. */
    Provided(1 | 2),

    /** Depend at runtime phase only. */
    System(1 | 2);

    /** The internal flag. */
    private final int bit;

    /**
     * <p>
     * Scope definition.
     * </p>
     * 
     * @param bit
     */
    private Scope(int bit) {
        this.bit = bit;
    }

    /**
     * <p>
     * Check whether this scope overlaps the specified scope or not.
     * </p>
     * 
     * @param scope
     * @return
     */
    public boolean contains(Scope scope) {
        return (bit & scope.bit) == scope.bit;
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
