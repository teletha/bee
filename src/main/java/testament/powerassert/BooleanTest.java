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
public class BooleanTest {

    @Rule
    public static final PowerAssertTester tester = new PowerAssertTester();

    @Test
    public void constant() throws Exception {
        boolean value = false;

        tester.willCapture("value", value);
        assert value;
    }

    @Test
    public void array() throws Exception {
        boolean[] array = {false, false, false};

        tester.willCapture("array", array);
        assert array == null;
    }

    @Test
    public void arrayIndex() throws Exception {
        boolean[] array = {false, false, false};

        tester.willCapture("array", array);
        tester.willCapture("array[1]", false);
        assert array[1];
    }

    @Test
    public void arrayLength() throws Exception {
        boolean[] array = {false, false, false};

        tester.willCapture("array", array);
        tester.willCapture("array.length", 3);
        assert array.length == 10;
    }

    @Test
    public void arrayNew() throws Exception {
        tester.willUse("new boolean[] {true, false}");
        assert new boolean[] {true, false} == null;
    }

    @Test
    public void varargs() throws Exception {
        tester.willCapture("var()", false);
        assert var();
    }

    boolean var(boolean... var) {
        return false;
    }

    @Test
    public void method() throws Exception {
        tester.willCapture("test()", false);
        assert test();
    }

    boolean test() {
        return false;
    }

    @Test
    public void parameter() throws Exception {
        tester.willCapture("test(false)", false);
        assert test(false);
    }

    private boolean test(boolean value) {
        return false;
    }

    /** The tester. */
    private boolean booleanField = false;

    /** The tester. */
    private static boolean booleanFieldStatic = false;

    @Test
    public void fieldBooleanAccess() throws Exception {
        tester.willCapture("booleanField", false);
        assert booleanField;
    }

    @Test
    public void fieldIntAccessWithHiddenName() throws Exception {
        boolean booleanField = false;

        tester.willCapture("this.booleanField", booleanField);
        assert this.booleanField;
    }

    @Test
    public void fieldBooleanStaticAccess() throws Exception {
        tester.willCapture("booleanFieldStatic", false);
        assert booleanFieldStatic;
    }
}
