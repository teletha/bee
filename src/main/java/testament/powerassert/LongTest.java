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
 * @version 2012/01/18 13:15:48
 */
public class LongTest {

    @Rule
    public static final PowerAssertTester tester = new PowerAssertTester();

    @Test
    public void constant_0() throws Exception {
        long value = -1;

        tester.willUse("0");
        tester.willCapture("value", value);
        assert 0 == value;
    }

    @Test
    public void constant_1() throws Exception {
        long value = -1;

        tester.willUse("1");
        tester.willCapture("value", value);
        assert 1 == value;
    }

    @Test
    public void constant_2() throws Exception {
        long value = -1;

        tester.willUse("2");
        tester.willCapture("value", value);
        assert 2 == value;
    }

    @Test
    public void constant_3() throws Exception {
        long value = -1;

        tester.willUse("3");
        tester.willCapture("value", value);
        assert 3 == value;
    }

    @Test
    public void constant_M1() throws Exception {
        long value = 0;

        tester.willUse("-1");
        tester.willCapture("value", value);
        assert -1 == value;
    }

    @Test
    public void big() throws Exception {
        long value = 2;

        tester.willUse("1234567890123");
        tester.willCapture("value", value);
        assert 1234567890123L == value;
    }

    @Test
    public void negative() throws Exception {
        long value = 10;

        tester.willUse("10");
        tester.willUse("-value");
        tester.willCapture("value", value);
        assert 10 == -value;
    }

    @Test
    public void array() throws Exception {
        long[] array = {0, 1, 2};

        tester.willCapture("array", array);
        assert array == null;
    }

    @Test
    public void arrayIndex() throws Exception {
        long[] array = {0, 1, 2};

        tester.willCapture("array", array);
        tester.willCapture("array[1]", 1L);
        assert array[1] == 128;
    }

    @Test
    public void arrayLength() throws Exception {
        long[] array = {0, 1, 2};

        tester.willCapture("array", array);
        tester.willCapture("array.length", 3);
        assert array.length == 10;
    }

    @Test
    public void arrayNew() throws Exception {
        tester.willUse("new long[] {1, 2}");
        assert new long[] {1, 2} == null;
    }

    @Test
    public void varargs() throws Exception {
        tester.willCapture("var()", false);
        assert var();
    }

    boolean var(long... var) {
        return false;
    }

    @Test
    public void method() throws Exception {
        tester.willCapture("test()", 1L);
        assert test() == 2;
    }

    long test() {
        return 1;
    }

    @Test
    public void parameter() throws Exception {
        tester.willCapture("test(12)", false);
        assert test(12);
    }

    private boolean test(long value) {
        return false;
    }

    /** The tester. */
    private long longField = 11;

    /** The tester. */
    private static long longFieldStatic = 11;

    @Test
    public void fieldLongAccess() throws Exception {
        tester.willCapture("longField", 11L);
        assert longField == 0;
    }

    @Test
    public void fieldIntAccessWithHiddenName() throws Exception {
        long longField = 11;

        tester.willCapture("this.longField", longField);
        assert this.longField == 0;
    }

    @Test
    public void fieldLongStaticAccess() throws Exception {
        tester.willCapture("longFieldStatic", 11L);
        assert longFieldStatic == 0;
    }
}
