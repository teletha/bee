/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package testament.powerassert;

import org.junit.Rule;
import org.junit.Test;

/**
 * @version 2012/01/19 15:08:05
 */
public class ThrowTest {

    @Rule
    public static final PowerAssertTester tester = new PowerAssertTester();

    @Test
    public void useAssertWithMessage() {
        int value = 4;

        tester.willCapture("value", value);
        assert value == -1 : "this value is " + value;
    }

    @Test
    public void useAssertWithIntMessage() {
        int value = 4;

        tester.willCapture("value", value);
        assert value == -1 : value;
    }
}
