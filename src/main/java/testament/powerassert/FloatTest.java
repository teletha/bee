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
public class FloatTest {

    @Rule
    public static final PowerAssertTester tester = new PowerAssertTester();

    @Test
    public void constant_0() throws Exception {
        float value = -1;

        tester.willUse("0");
        tester.willCapture("value", value);
        assert 0 == value;
    }

    @Test
    public void constant_1() throws Exception {
        float value = -1;

        tester.willUse("1");
        tester.willCapture("value", value);
        assert 1 == value;
    }

    @Test
    public void constant_2() throws Exception {
        float value = -1;

        tester.willUse("2");
        tester.willCapture("value", value);
        assert 2 == value;
    }

    @Test
    public void constant_3() throws Exception {
        float value = -1;

        tester.willUse("3");
        tester.willCapture("value", value);
        assert 3 == value;
    }

    @Test
    public void constant_M1() throws Exception {
        float value = 0;

        tester.willUse("-1");
        tester.willCapture("value", value);
        assert -1 == value;
    }

    @Test
    public void big() throws Exception {
        float value = 2;

        tester.willUse("0.12345678");
        tester.willCapture("value", value);
        assert 0.12345678f == value;
    }

    @Test
    public void negative() throws Exception {
        float value = 0.3f;

        tester.willUse("0.3");
        tester.willUse("-value");
        tester.willCapture("value", value);
        assert 0.3f == -value;
    }

    @Test
    public void array() throws Exception {
        float[] array = {0, 1, 2};

        tester.willCapture("array", array);
        assert array == null;
    }

    @Test
    public void arrayIndex() throws Exception {
        float[] array = {0, 1, 2};

        tester.willCapture("array", array);
        tester.willCapture("array[1]", 1f);
        assert array[1] == 128;
    }

    @Test
    public void arrayLength() throws Exception {
        float[] array = {0, 1, 2};

        tester.willCapture("array", array);
        tester.willCapture("array.length", 3);
        assert array.length == 10;
    }

    @Test
    public void arrayNew() throws Exception {
        tester.willUse("new float[] {1.0, 2.0}");
        assert new float[] {1, 2} == null;
    }

    @Test
    public void varargs() throws Exception {
        tester.willCapture("var()", false);
        assert var();
    }

    boolean var(float... var) {
        return false;
    }

    @Test
    public void method() throws Exception {
        tester.willCapture("test()", 1f);
        assert test() == 2f;
    }

    float test() {
        return 1;
    }

    @Test
    public void parameter() throws Exception {
        tester.willCapture("test(12.0)", false);
        assert test(12);
    }

    private boolean test(float value) {
        return false;
    }

    /** The tester. */
    private float floatField = 0.123f;

    /** The tester. */
    private static float floatFieldStatic = 0.123f;

    @Test
    public void fieldFloatAccess() throws Exception {
        tester.willCapture("floatField", 0.123f);
        assert floatField == 0;
    }

    @Test
    public void fieldIntAccessWithHiddenName() throws Exception {
        float floatField = 0.123f;

        tester.willCapture("this.floatField", floatField);
        assert this.floatField == 0;
    }

    @Test
    public void fieldFloatStaticAccess() throws Exception {
        tester.willCapture("floatFieldStatic", 0.123f);
        assert floatFieldStatic == 0;
    }
}
