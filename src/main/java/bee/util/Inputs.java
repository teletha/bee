/*
 * Copyright (C) 2025 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.util;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import javax.lang.model.SourceVersion;

import bee.UserInterface;
import kiss.I;
import kiss.Observer;
import psychopath.File;
import psychopath.Location;
import psychopath.Locator;
import psychopath.Progress;

public class Inputs {

    public static Observer<Progress> observerFor(UserInterface ui, Location target, String progressMessage, String completeMessage) {
        return new Observer<>() {

            /** reduce log flow */
            private long latest;

            /**
             * {@inheritDoc}
             */
            @Override
            public void accept(Progress info) {
                long now = System.nanoTime();
                if (66000000 <= now - latest) {
                    latest = now;
                    ui.trace(progressMessage, ": ", info.completedFiles(), "/", info.totalFiles, " (", info.rateByFiles(), "%)");
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void error(Throwable e) {
                ui.error(e);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void complete() {
                if (target == null) {
                    ui.info(completeMessage);
                } else if (target.isDirectory()) {
                    ui.info(completeMessage, ": ", target);
                } else {
                    ui.info(completeMessage, ": ", target, " (", formatAsSize(target.size()), ")");
                }
            }
        };
    }

    public static String format(Object p1, String text) {
        return String.format(text, p1);
    }

    /**
     * Format as human-readable size.
     * 
     * @param bytes
     * @return
     */
    public static String formatAsSize(long bytes) {
        return formatAsSize(bytes, true);
    }

    /**
     * Format as human-readable size.
     * 
     * @param bytes
     * @return
     */
    public static String formatAsSize(long bytes, boolean unit) {
        double kb = bytes / 1024.0;
        if (kb < 0.1) {
            return Long.toString(bytes).concat(unit ? "Bytes" : "");
        }

        double mb = kb / 1024.0;
        if (mb < 0.9) {
            return formatAsSize(kb, unit ? "KB" : "");
        }

        double gb = mb / 1024.0;
        if (gb < 0.9) {
            return formatAsSize(mb, unit ? "MB" : "");
        }

        double tb = gb / 1024.0;
        if (tb < 0.9) {
            return formatAsSize(gb, unit ? "GB" : "");
        }
        return formatAsSize(tb, unit ? "TB" : "");
    }

    /**
     * Remove tailing zero.
     * 
     * @param size
     * @param unit
     * @return
     */
    private static String formatAsSize(double size, String unit) {
        long rouded = (long) size;
        if (rouded == size) {
            return Long.toString(rouded).concat(unit);
        } else {
            return String.format("%.2f", size).concat(unit);
        }
    }

    /**
     * Bee template helper.
     * 
     * @param template
     * @param context
     * @return
     */
    public static String template(String template, Object... context) {
        return I.express(template, context, (m, o, p) -> {
            try {
                return o.getClass().getField(p).get(o);
            } catch (Exception e) {
                try {
                    return o.getClass().getMethod(p).invoke(o);
                } catch (Exception x) {
                    return null;
                }
            }
        });
    }

    /**
     * Bee template helper.
     * 
     * @param template
     * @param context
     * @return
     */
    public static List<String> templates(String template, Object... context) {
        return I.list(template(template, context).split("\\R"));
    }

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

        default:
            return version.name().substring(8);
        }
    }

    /**
     * Capitalize user input.
     * 
     * @param input A user input.
     * @return A capitalized input.
     */
    public static String capitalize(String input) {
        char[] chars = input.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
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

            if (i != 0 && Character.isUpperCase(c) && Character.isLowerCase(input.charAt(i - 1))) {
                builder.append('-');
            }
            builder.append(Character.toLowerCase(c));
        }
        return builder.toString();
    }

    /**
     * Selects the string that most closely resembles the specified string from a list of
     * candidates.
     * 
     * @param input
     * @param candidates
     * @return
     */
    public static String recommend(String input, Collection<String> candidates) {
        float min = 100;
        String text = null;
        for (String candidate : candidates) {
            float distance = calculateDistance(input, candidate);
            if (distance <= 0.1) {
                return candidate;
            } else if (distance < min) {
                min = distance;
                text = candidate;
            }
        }

        return 0.4 <= min ? null : text;
    }

    /**
     * Calculate the Levenshtein distance between two strings.
     *
     * @param one the first string to be compared, not null
     * @param other the second string to be compared, not null
     * @return the distance between two strings
     */
    private static float calculateDistance(CharSequence one, CharSequence other) {
        int oneLength = one.length();
        int otherLength = other.length();
        float[][] distance = new float[otherLength + 1][oneLength + 1];

        // calculate
        for (int i = 1; i <= oneLength; ++i) {
            distance[0][i] = i;
        }
        for (int i = 1; i <= otherLength; ++i) {
            distance[i][0] = i;
        }
        for (int i = 1; i <= otherLength; ++i) {
            for (int j = 1; j <= oneLength; ++j) {
                if (one.charAt(j - 1) == other.charAt(i - 1)) {
                    distance[i][j] = distance[i - 1][j - 1];
                } else {
                    distance[i][j] = Math.min(distance[i - 1][j - 1] + 1, Math.min(distance[i - 1][j] + 1, distance[i][j - 1] + 1));
                }
            }
        }

        return distance[otherLength][oneLength] / Math.max(oneLength, otherLength);
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

            private String text = file.text().strip();

            /**
             * Update contents.
             */
            private void update() {
                long time = file.lastModifiedMilli();
                if (modified != time) {
                    modified = time;
                    text = file.text().strip();
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