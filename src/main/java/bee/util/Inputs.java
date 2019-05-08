/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.util;

import javax.lang.model.SourceVersion;

/**
 * @version 2016/10/12 14:32:06
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

    /**
     * <p>
     * Normalize {@link SourceVersion} to human-readable version number.
     * </p>
     * 
     * @param version A target version.
     * @return A version number.
     */
    public static String normalize(SourceVersion version) {
        if (version == null) {
            version = SourceVersion.latest();
        }

        switch (version) {
        case RELEASE_0:
            return "1.0";

        case RELEASE_1:
            return "1.1";

        case RELEASE_2:
            return "1.2";

        case RELEASE_3:
            return "1.3";

        case RELEASE_4:
            return "1.4";

        case RELEASE_5:
            return "1.5";

        case RELEASE_6:
            return "1.6";

        case RELEASE_7:
            return "7";

        case RELEASE_8:
            return "8";

        case RELEASE_9:
            return "9";

        case RELEASE_10:
            return "10";

        case RELEASE_11:
            return "11";

        case RELEASE_12:
            return "12";

        default:
            return "12";
        }
    }

    /**
     * <p>
     * Hyphenize user input.
     * </p>
     * 
     * @param input A user input.
     * @return A hyphenized input.
     */
    public static String hyphenize(String input) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (i != 0 && Character.isUpperCase(c)) {
                builder.append('-');
            }
            builder.append(Character.toLowerCase(c));
        }
        return builder.toString();
    }
}
