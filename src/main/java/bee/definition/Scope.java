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

    COMPILE,

    TEST,

    PROVIDED,

    RUNTIME,

    SYSTEM;

    /**
     * <p>
     * Find scope by keyword.
     * </p>
     * 
     * @param keyword
     * @return
     */
    public static Scope by(String keyword) {
        if (keyword == null || keyword.length() == 0) {
            return COMPILE;
        }
        return valueOf(keyword.toUpperCase());
    }
}
