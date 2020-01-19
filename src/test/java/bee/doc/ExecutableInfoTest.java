/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.doc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

public class ExecutableInfoTest extends JavadocTestSupport {

    @Test
    public void parameter0() {
        ExecutableInfo info = currentMethod();
        assert info.name.equals("parameter0");
        assert info.numberOfParameters() == 0;
    }

    @ParameterizedTest
    @ArgumentsSource(NullProvider.class)
    public void parameter1(String value) {
        assert checkParamName(currentMethod(), "value");
    }

    @ParameterizedTest
    @ArgumentsSource(NullProvider.class)
    public void parameter2(String value, String text) {
        assert checkParamName(currentMethod(), "value", "text");
    }

    @ParameterizedTest
    @ArgumentsSource(NullProvider.class)
    public void parameter3(String value, String text, Object context) {
        assert checkParamName(currentMethod(), "value", "text", "context");
    }

    /**
     * Shortcut method.
     * 
     * @param info
     * @param expected
     * @return
     */
    private boolean checkParamName(ExecutableInfo info, String... expected) {
        for (int i = 0; i < expected.length; i++) {
            assert info.createParameterName(i).text().equals(expected[i]);
        }
        return true;
    }
}
