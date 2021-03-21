/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.util;

import java.nio.file.Path;

import javax.lang.model.SourceVersion;

import psychopath.File;
import psychopath.Locator;

public class Inputs {

    /**
     * Normalize user input.
     * 
     * @param input A user input.
     * @param defaultValue A default value.
     * @return A normalized input.
     */
    public static String normalize(CharSequence input, String defaultValue) {
        if (input == null) {
            input = defaultValue;
        }

        // trim whitespcae
        input = input.toString().trim();

        if (input.length() == 0) {
            input = defaultValue;
        }

        // API definition
        return input.toString();
    }

    /**
     * Normalize {@link SourceVersion} to human-readable version number.
     * 
     * @param version A target version.
     * @return A version number.
     */
    public static String normalize(SourceVersion version) {
        return normalize(version, null);
    }

    /**
     * Normalize {@link SourceVersion} to human-readable version number.
     * 
     * @param version A target version.
     * @return A version number.
     */
    public static String normalize(SourceVersion version, SourceVersion max) {
        if (version == null) {
            version = SourceVersion.latest();
        }

        if (max != null && version.compareTo(max) > 0) {
            version = max;
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

        default:
            return version.name().substring(8);
        }
    }

    /**
     * Hyphenize user input.
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

    /**
     * Return the reference of the specified file's text.
     * 
     * @param file A path to the target text file.
     * @return A file contents.
     */
    public static CharSequence ref(String file) {
        return ref(Locator.file(file));
    }

    /**
     * Return the reference of the specified file's text.
     * 
     * @param file A target text file.
     * @return A file contents.
     */
    public static CharSequence ref(Path file) {
        return ref(Locator.file(file));
    }

    /**
     * Return the reference of the specified file's text.
     * 
     * @param file A target text file.
     * @return A file contents.
     */
    public static CharSequence ref(File file) {
        return new CharSequence() {

            private long modified = file.lastModifiedMilli();

            private String text = file.text();

            /**
             * Update contents.
             */
            private void update() {
                long time = file.lastModifiedMilli();
                if (modified != time) {
                    modified = time;
                    text = file.text();
                }
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                update();
                return text.subSequence(start, end);
            }

            @Override
            public int length() {
                update();
                return text.length();
            }

            @Override
            public char charAt(int index) {
                update();
                return text.charAt(index);
            }

            @Override
            public int hashCode() {
                update();
                return text.hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                update();
                return text.equals(obj);
            }

            @Override
            public String toString() {
                update();
                return text.toString();
            }
        };
    }
}