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
public class IntTest {

    @Rule
    public static final PowerAssertTester tester = new PowerAssertTester();

    @Test
    public void constant_0() throws Exception {
        int value = -1;

        tester.willUse("0");
        tester.willCapture("value", value);
        assert 0 == value;
    }

    @Test
    public void constant_1() throws Exception {
        int value = -1;

        tester.willUse("1");
        tester.willCapture("value", value);
        assert 1 == value;
    }

    @Test
    public void constant_2() throws Exception {
        int value = -1;

        tester.willUse("2");
        tester.willCapture("value", value);
        assert 2 == value;
    }

    @Test
    public void constant_3() throws Exception {
        int value = -1;

        tester.willUse("3");
        tester.willCapture("value", value);
        assert 3 == value;
    }

    @Test
    public void constant_M1() throws Exception {
        int value = 0;

        tester.willUse("-1");
        tester.willCapture("value", value);
        assert -1 == value;
    }

    @Test
    public void big() throws Exception {
        int value = 2;

        tester.willUse("123456789");
        tester.willCapture("value", value);
        assert 123456789 == value;
    }

    @Test
    public void negative() throws Exception {
        int value = 10;

        tester.willUse("10");
        tester.willUse("-value");
        tester.willCapture("value", value);
        assert 10 == -value;
    }

    @Test
    public void array() throws Exception {
        int[] array = {0, 1, 2};

        tester.willCapture("array", array);
        assert array == null;
    }

    @Test
    public void arrayIndex() throws Exception {
        int[] array = {0, 1, 2};

        tester.willCapture("array", array);
        tester.willCapture("array[1]", 1);
        assert array[1] == 128;
    }

    @Test
    public void arrayLength() throws Exception {
        int[] array = {0, 1, 2};

        tester.willCapture("array", array);
        tester.willCapture("array.length", 3);
        assert array.length == 10;
    }

    @Test
    public void arrayNew() throws Exception {
        tester.willUse("new int[] {1, 2}");
        assert new int[] {1, 2} == null;
    }

    @Test
    public void varargs() throws Exception {
        tester.willCapture("var()", false);
        assert var();
    }

    boolean var(int... var) {
        return false;
    }

    @Test
    public void varargsWithHead() throws Exception {
        tester.willCapture("head(1)", false);
        assert head(1);
    }

    boolean head(int head, int... var) {
        return false;
    }

    @Test
    public void method() throws Exception {
        tester.willCapture("test()", 1);
        assert test() == 2;
    }

    int test() {
        return 1;
    }

    @Test
    public void parameter() throws Exception {
        tester.willCapture("test(12)", false);
        assert test(12);
    }

    private boolean test(int value) {
        return false;
    }

    /** The tester. */
    private int intField = 11;

    /** The tester. */
    private static int intFieldStatic = 11;

    @Test
    public void fieldIntAccess() throws Exception {
        tester.willCapture("intField", 11);
        assert intField == 0;
    }

    @Test
    public void fieldIntAccessWithHiddenName() throws Exception {
        int intField = 11;

        tester.willCapture("this.intField", intField);
        assert this.intField == 0;
    }

    @Test
    public void fieldIntStaticAccess() throws Exception {
        tester.willCapture("intFieldStatic", 11);
        assert intFieldStatic == 0;
    }
}
