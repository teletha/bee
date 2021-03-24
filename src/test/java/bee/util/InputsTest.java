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
}