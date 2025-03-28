/*
 * Copyright (C) 2025 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.coder;

import java.util.List;

import bee.api.License;
import kiss.Extensible;

/**
 * @version 2015/06/17 13:33:00
 */
public interface HeaderStyle extends Extensible {

    /**
     * Tells if the given content line must be skipped according to this header definition. The
     * header is outputted after any skipped line if any pattern defined on this point or on the
     * first line if not pattern defined.
     *
     * @param line The line to test.
     * @return true if this line must be skipped or false.
     */
    boolean isSkipLine(String line);

    /**
     * Tells if the given content line is the first line of a possible header of this definition
     * kind.
     *
     * @param line The line to test.
     * @return true if the first line of a header have been recognized or false.
     */
    boolean isFirstHeaderLine(String line);

    /**
     * Tells if the given content line is the end line of a possible header of this definition kind.
     *
     * @param line The end to test.
     * @return true if the end line of a header have been recognized or false.
     */
    boolean isEndHeaderLine(String line);

    /**
     * Build decorated header text by using the specified law header text.
     */
    List<String> decorate(List<String> header);

    /**
     * <p>
     * Apply the specified license header to the specified source.
     * </p>
     * 
     * @param source A source to update.
     * @param license A license definition.
     * @return An updated source.
     */
    default List<String> convert(List<String> source, License license) {
        // ignore empty source
        if (source == null || source.isEmpty()) {
            return null;
        }

        int first = -1;
        int end = -1;

        // find first header line
        for (int i = 0, size = source.size() - 1; i <= size; i++) {
            String line = source.get(i);

            if (line.trim().isEmpty()) {
                // skip blank line
                if (i == size) {
                    // if it is last line, this souce is blank. so we must ignore it.
                    return null;
                }
            } else if (isFirstHeaderLine(line)) {
                first = i;
                break;
            } else if (isSkipLine(line)) {
                // skip line
            } else {
                break;
            }
        }

        if (first == -1) {
            first = 0;
        } else {
            // find end header line
            for (int i = first + 1; i < source.size(); i++) {
                String line = source.get(i);

                if (isEndHeaderLine(line)) {
                    end = i;
                    break;
                }
            }
        }

        List<String> decorated = decorate(license.text(true));

        // remove existing header
        if (end != -1) {
            // check header equality
            if (decorated.equals(source.subList(first, end + 1))) {
                // if this souce has no modification, we must ignore it.
                return null;
            }
            for (int i = end; first <= i; i--) {
                source.remove(i);
            }
        }

        // add specified header
        source.addAll(first, decorate(license.text(true)));

        // API definition
        return source;
    }
}