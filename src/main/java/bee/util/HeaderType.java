/*
 * Copyright (C) 2015 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.util;

import java.util.List;

import kiss.Extensible;

/**
 * @version 2015/06/17 13:33:00
 */
public interface HeaderType extends Extensible {

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
     * Build header text by using the specified text.
     */
    List<String> text(List<String> text);
}
