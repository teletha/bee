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

/**
 * @version 2012/04/01 15:57:20
 */
public class Inputs {

    /**
     * <p>
     * Normalize user input.
     * </p>
     * 
     * @param input A user input.
     * @param defaultValue A default value.
     * @return A normalized input.
     */
    public static String normalize(String input, String defaultValue) {
        if (input == null) {
            input = defaultValue;
        }

        // trim whitespcae
        input = input.trim();

        if (input.length() == 0) {
            input = defaultValue;
        }

        // API definition
        return input;
    }
}
