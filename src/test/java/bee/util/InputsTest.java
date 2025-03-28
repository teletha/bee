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

import java.util.Set;

import org.junit.jupiter.api.Test;

class InputsTest {

    @Test
    void formatAsSize() {
        assert Inputs.formatAsSize(100).equals("100Bytes");
        assert Inputs.formatAsSize(5 * 1024).equals("5KB");
        assert Inputs.formatAsSize(1536).equals("1.50KB");
        assert Inputs.formatAsSize(1538).equals("1.50KB");
        assert Inputs.formatAsSize(1545).equals("1.51KB");
        assert Inputs.formatAsSize(1555).equals("1.52KB");
    }

    @Test
    void formatAsSizeWithoutUnit() {
        assert Inputs.formatAsSize(100, false).equals("100");
        assert Inputs.formatAsSize(5 * 1024, false).equals("5");
        assert Inputs.formatAsSize(1536, false).equals("1.50");
        assert Inputs.formatAsSize(1538, false).equals("1.50");
        assert Inputs.formatAsSize(1545, false).equals("1.51");
        assert Inputs.formatAsSize(1555, false).equals("1.52");
    }

    @Test
    void hyphenize() {
        assert Inputs.hyphenize("ok").equals("ok");
        assert Inputs.hyphenize("OK").equals("ok");
        assert Inputs.hyphenize("DoSomething").equals("do-something");
        assert Inputs.hyphenize("testXML").equals("test-xml");
        assert Inputs.hyphenize("CI").equals("ci");
    }

    @Test
    void recommend() {
        assert Inputs.recommend("clear", Set.of("clean", "unclear", "crest", "cool", "clover")).equals("clean");
        assert Inputs.recommend("en", Set.of("environment", "env", "enter", "cent", "tend")).equals("env");
    }
}