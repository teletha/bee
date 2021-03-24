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

import static bee.Platform.EOL;
import static bee.util.Notation.INDENT;

import org.junit.jupiter.api.Test;

class NotationTest {

    @Test
    void title() {
        Notation notation = new Notation();
        notation.title("title");
        assert notation.toString().equals("title" + EOL + "=====" + EOL);
    }

    @Test
    void paragraph() {
        Notation notation = new Notation();
        notation.p("paragraph");
        assert notation.toString().equals("paragraph" + EOL + EOL);
    }

    @Test
    void paragraphMultiLines() {
        Notation notation = new Notation();
        notation.p("first line" + EOL + "second line");
        assert notation.toString().equals("first line" + EOL + "second line" + EOL + EOL);
    }

    @Test
    void section() {
        Notation notation = new Notation();
        notation.section(() -> {
            notation.p("nest" + EOL + "line");
        });
        assert notation.toString().equals(INDENT + "nest" + EOL + INDENT + "line" + EOL + EOL);
    }
}