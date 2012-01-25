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
public class ObjectTest {

    @Rule
    public static final PowerAssertTester tester = new PowerAssertTester();

    @Test
    public void constant() throws Exception {
        Object value = new Object();

        tester.willCapture("value", value);
        assert null == value;
    }

    @Test
    public void array() throws Exception {
        Object[] array = {"0", "1", "2"};

        tester.willCapture("array", array);
        assert array == null;
    }

    @Test
    public void arrayIndex() throws Exception {
        Object[] array = {"0", "1", "2"};

        tester.willCapture("array", array);
        tester.willCapture("array[1]", "1");
        assert array[1] == "128";
    }

    @Test
    public void arrayLength() throws Exception {
        Object[] array = {"0", "1", "2"};

        tester.willCapture("array", array);
        tester.willCapture("array.length", 3);
        assert array.length == 10;
    }

    @Test
    public void arrayNew() throws Exception {
        tester.willUse("new Object[] {\"1\", \"2\"}");
        assert new Object[] {"1", "2"} == null;
    }

    @Test
    public void varargs() throws Exception {
        tester.willCapture("var()", false);
        assert var();
    }

    boolean var(Object... var) {
        return false;
    }

    @Test
    public void varargsWithHead() throws Exception {
        tester.willCapture("head(\"1\")", false);
        assert head("1");
    }

    boolean head(String head, Object... var) {
        return false;
    }

    @Test
    public void method() throws Exception {
        tester.willCapture("test()", "1");
        assert test() == "2";
    }

    Object test() {
        return "1";
    }

    @Test
    public void parameter() throws Exception {
        tester.willCapture("test(\"12\")", false);
        assert test("12");
    }

    private boolean test(Object value) {
        return false;
    }

    @Test
    public void parameterWithVarArg() throws Exception {
        tester.willCapture("var()", false);
        assert var();
    }

    private boolean var(String... vars) {
        return false;
    }

    /** The tester. */
    private Object ObjectField = "11";

    /** The tester. */
    private static Object ObjectFieldStatic = "11";

    @Test
    public void fieldObjectAccess() throws Exception {
        tester.willCapture("ObjectField", "11");
        assert ObjectField == "";
    }

    @Test
    public void fieldObjectStaticAccess() throws Exception {
        tester.willCapture("ObjectFieldStatic", "11");
        assert ObjectFieldStatic == "";
    }
}
