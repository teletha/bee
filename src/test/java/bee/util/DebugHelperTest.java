/*
 * Copyright (C) 2020 Nameless Production Committee
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

class DebugHelperTest {

    @Test
    void name() {
        assert DebugHelper.$(this::name).equals(DebugHelperTest.class.getName() + "#name()");
        assert DebugHelper.$(DebugHelperTest::name).equals(DebugHelperTest.class.getName() + "#name()");
        assert DebugHelper.$(String::length).equals("java.lang.String#length()");
        assert DebugHelper.$(String::concat).equals("java.lang.String#concat(String)");
        assert DebugHelper.$(String::replaceAll).equals("java.lang.String#replaceAll(String, String)");
        assert DebugHelper.$(Map<?, ?>::compute).equals("java.util.Map#compute(Object, BiFunction)");
        assert DebugHelper.$(Consumer<?>::accept).equals("java.util.function.Consumer#accept(Object)");
        assert DebugHelper.$(BiConsumer<?, ?>::accept).equals("java.util.function.BiConsumer#accept(Object, Object)");
    }
}