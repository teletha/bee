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
public class DoubleTest {

    @Rule
    public static final PowerAssertTester tester = new PowerAssertTester();

    @Test
    public void constant_0() throws Exception {
        double value = -1;

        tester.willUse("0");
        tester.willCapture("value", value);
        assert 0 == value;
    }

    @Test
    public void constant_1() throws Exception {
        double value = -1;

        tester.willUse("1");
        tester.willCapture("value", value);
        assert 1 == value;
    }

    @Test
    public void constant_2() throws Exception {
        double value = -1;

        tester.willUse("2");
        tester.willCapture("value", value);
        assert 2 == value;
    }

    @Test
    public void constant_3() throws Exception {
        double value = -1;

        tester.willUse("3");
        tester.willCapture("value", value);
        assert 3 == value;
    }

    @Test
    public void constant_M1() throws Exception {
        double value = 0;

        tester.willUse("-1");
        tester.willCapture("value", value);
        assert -1 == value;
    }

    @Test
    public void big() throws Exception {
        double value = 2;

        tester.willUse("0.1234567898765432");
        tester.willCapture("value", value);
        assert 0.1234567898765432d == value;
    }

    @Test
    public void negative() throws Exception {
        double value = 0.3;

        tester.willUse("0.3");
        tester.willUse("-value");
        tester.willCapture("value", value);
        assert 0.3 == -value;
    }

    @Test
    public void array() throws Exception {
        double[] array = {0, 1, 2};

        tester.willCapture("array", array);
        assert array == null;
    }

    @Test
    public void arrayIndex() throws Exception {
        double[] array = {0, 1, 2};

        tester.willCapture("array", array);
        tester.willCapture("array[1]", 1d);
        assert array[1] == 128;
    }

    @Test
    public void arrayLength() throws Exception {
        double[] array = {0, 1, 2};

        tester.willCapture("array", array);
        tester.willCapture("array.length", 3);
        assert array.length == 10;
    }

    @Test
    public void arrayNew() throws Exception {
        tester.willUse("new double[] {1.0, 2.0}");
        assert new double[] {1, 2} == null;
    }

    @Test
    public void varargs() throws Exception {
        tester.willCapture("var()", false);
        assert var();
    }

    boolean var(double... var) {
        return false;
    }

    @Test
    public void method() throws Exception {
        tester.willCapture("test()", 1d);
        assert test() == 2;
    }

    double test() {
        return 1;
    }

    @Test
    public void parameter() throws Exception {
        tester.willCapture("test(0.123456)", false);
        assert test(0.123456d);
    }

    private boolean test(double value) {
        return false;
    }

    /** The tester. */
    private double doubleField = 32.1011d;

    /** The tester. */
    private static double doubleFieldStatic = 32.1011d;

    @Test
    public void fieldDoubleAccess() throws Exception {
        tester.willCapture("doubleField", 32.1011d);
        assert doubleField == 0;
    }

    @Test
    public void fieldIntAccessWithHiddenName() throws Exception {
        double doubleField = 32.1011d;

        tester.willCapture("this.doubleField", doubleField);
        assert this.doubleField == 0;
    }

    @Test
    public void fieldDoubleStaticAccess() throws Exception {
        tester.willCapture("doubleFieldStatic", 32.1011d);
        assert doubleFieldStatic == 0;
    }
}
