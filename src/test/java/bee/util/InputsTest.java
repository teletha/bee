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

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

class InputsTest {

    @Test
    void formatAsSize() {
        assert Inputs.formatAsSize(100).equals("100Bytes");
        assert Inputs.formatAsSize(5 * 1024).equals("5KB");
        assert Inputs.formatAsSize(1536).equals("1.5KB");
        assert Inputs.formatAsSize(1538).equals("1.5KB");
        assert Inputs.formatAsSize(1545).equals("1.51KB");
        assert Inputs.formatAsSize(1555).equals("1.52KB");
    }

    @Test
    void formatAsSizeWithoutUnit() {
        assert Inputs.formatAsSize(100, false).equals("100");
        assert Inputs.formatAsSize(5 * 1024, false).equals("5");
        assert Inputs.formatAsSize(1536, false).equals("1.5");
        assert Inputs.formatAsSize(1538, false).equals("1.5");
        assert Inputs.formatAsSize(1545, false).equals("1.51");
        assert Inputs.formatAsSize(1555, false).equals("1.52");
    }

    @Test
    void signature() {
        assert Inputs.signature(this::signature).equals(InputsTest.class.getName() + "#signature()");
        assert Inputs.signature(InputsTest::signature).equals(InputsTest.class.getName() + "#signature()");
        assert Inputs.signature(String::length).equals("java.lang.String#length()");
        assert Inputs.signature(String::concat).equals("java.lang.String#concat(String)");
        assert Inputs.signature(String::replaceAll).equals("java.lang.String#replaceAll(String, String)");
        assert Inputs.signature(Map<?, ?>::compute).equals("java.util.Map#compute(Object, BiFunction)");
        assert Inputs.signature(Consumer<?>::accept).equals("java.util.function.Consumer#accept(Object)");
        assert Inputs.signature(BiConsumer<?, ?>::accept).equals("java.util.function.BiConsumer#accept(Object, Object)");
    }

    @Test
    void hyphenize() {
        assert Inputs.hyphenize("ok").equals("ok");
        assert Inputs.hyphenize("OK").equals("ok");
        assert Inputs.hyphenize("DoSomething").equals("do-something");
        assert Inputs.hyphenize("testXML").equals("test-xml");
        assert Inputs.hyphenize("CI").equals("ci");
    }
}