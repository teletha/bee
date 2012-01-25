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

import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

/**
 * @version 2012/01/10 9:53:52
 */
public class PowerAssertTest {

    @Rule
    public static final PowerAssertTester test = new PowerAssertTester();

    @Test
    public void shortConstantAndVariable() throws Exception {
        short value = 2;

        test.willUse("1");
        test.willCapture("value", (int) value);
        assert (short) 1 == value;
    }

    @Test
    public void shortBigConstantAndVariable() throws Exception {
        short value = 2;

        test.willUse("128");
        test.willCapture("value", (int) value);
        assert (short) 128 == value;
    }

    @Test
    public void nullLiteral1() throws Exception {
        String value = "";

        test.willCapture("value", value);
        assert value == null;
    }

    @Test
    public void nullLiteral2() throws Exception {
        String value = null;

        test.willCapture("value", value);
        assert value != null;
    }

    @Test
    public void nullValue() throws Exception {
        String value = null;

        test.willCapture("value", value);
        assert value == "test";
    }

    @Test
    public void classLiteral() throws Exception {
        Class value = int.class;

        test.willCapture("value", value);
        test.willUse("Integer.class");
        assert Integer.class == value;
    }

    @Test
    public void classLiteralWithMethodCall() throws Exception {
        test.willCapture("Integer.class.getName()", "java.lang.Integer");
        assert Integer.class.getName() == "fail";
    }

    @Test
    public void enumLiteral() throws Exception {
        RetentionPolicy value = RetentionPolicy.CLASS;

        test.willCapture("value", RetentionPolicy.CLASS);
        test.willCapture("RetentionPolicy.RUNTIME", RetentionPolicy.RUNTIME);
        assert RetentionPolicy.RUNTIME == value;
    }

    @Test
    public void methodCall() throws Exception {
        String value = "test";

        test.willCapture("value", value);
        test.willCapture("value.equals(\"a\")", false);
        assert value.equals("a");
    }

    @Test
    public void privateMethodCall() throws Exception {
        assert privateMethod();
    }

    private boolean privateMethod() {
        return false;
    }

    @Test
    public void methodCalls() throws Exception {
        String value = "test";

        test.willCapture("value", value);
        test.willCapture("value.substring(2)", "st");
        test.willCapture("value.substring(2).equals(\"xx\")", false);
        assert value.substring(2).equals("xx");
    }

    @Test
    public void methodCallInt() throws Exception {
        List list = new ArrayList();
        list.add("a");
        list.add("b");
        list.add("c");

        test.willCapture("list", list);
        test.willCapture("list.size()", 3);
        assert list.size() == 34;
    }

    @Test
    public void methodCallObject() throws Exception {
        List list = new ArrayList();
        list.add("a");
        list.add("b");
        list.add("c");

        test.willCapture("list", list);
        test.willCapture("list.get(1)", "b");
        assert list.get(1) == "fail";
    }

    @Test
    public void methodStaticCall() throws Exception {
        Object value = "";

        test.willCapture("Integer.valueOf(10)", new Integer(10));
        test.willCapture("value", "");
        assert Integer.valueOf(10) == value;
    }

    @Test
    public void lessThan() {
        int one = 10;
        int other = 20;

        test.willCapture("one", one);
        test.willCapture("other", other);
        test.willUse("<");
        assert other < one;
    }

    @Test
    public void lessEqual() {
        int one = 10;
        int other = 20;

        test.willCapture("one", one);
        test.willCapture("other", other);
        test.willUse("<=");
        assert other <= one;
    }

    @Test
    public void greaterThan() {
        int one = 10;
        int other = 20;

        test.willCapture("one", one);
        test.willCapture("other", other);
        test.willUse(">");
        assert one > other;
    }

    @Test
    public void greaterEqual() {
        int one = 10;
        int other = 20;

        test.willCapture("one", one);
        test.willCapture("other", other);
        test.willUse(">=");
        assert one >= other;
    }

    @Test
    public void addition() {
        int one = 10;
        int other = 20;

        test.willCapture("one", one);
        test.willCapture("other", other);
        test.willUse("+");
        test.willUse("==");
        assert one + 1 == other;
    }

    @Test
    public void subtraction() {
        int one = 10;
        int other = 20;

        test.willCapture("one", one);
        test.willCapture("other", other);
        test.willUse("-");
        test.willUse("==");
        assert one - 1 == other;
    }

    @Test
    public void multiplication() {
        int one = 10;
        int other = 20;

        test.willCapture("one", one);
        test.willCapture("other", other);
        test.willUse("*");
        test.willUse("==");
        assert one * 3 == other;
    }

    @Test
    public void division() {
        int one = 10;
        int other = 20;

        test.willCapture("one", one);
        test.willCapture("other", other);
        test.willUse("/");
        test.willUse("==");
        assert one / 2 == other;
    }

    @Test
    public void remainder() {
        int one = 10;
        int other = 20;

        test.willCapture("one", one);
        test.willCapture("other", other);
        test.willUse("%");
        test.willUse("==");
        assert one % 2 == other;
    }

    @Test
    public void negative() {
        int one = 10;
        int other = 20;

        test.willCapture("one", one);
        test.willCapture("other", other);
        test.willUse("-");
        test.willUse("==");
        assert -one == other;
    }

    @Test
    public void leftShift() {
        int one = 10;
        int other = 20;

        test.willCapture("one", one);
        test.willCapture("other", other);
        test.willUse("<<");
        test.willUse("==");
        assert one << 3 == other;
    }

    @Test
    public void rightShift() {
        int one = 10;
        int other = 20;

        test.willCapture("one", one);
        test.willCapture("other", other);
        test.willUse(">>");
        test.willUse("==");
        assert one >> 3 == other;
    }

    @Test
    public void unrotateRightShift() {
        int one = 10;
        int other = 20;

        test.willCapture("one", one);
        test.willCapture("other", other);
        test.willUse(">>>");
        test.willUse("==");
        assert one >>> 3 == other;
    }

    @Test
    public void or() {
        int one = 10;
        int other = 20;

        test.willCapture("one", one);
        test.willCapture("other", other);
        test.willUse("|");
        test.willUse("==");
        assert (one | other) == other;
    }

    @Test
    public void xor() {
        int one = 10;
        int other = 20;

        test.willCapture("one", one);
        test.willCapture("other", other);
        test.willUse("^");
        test.willUse("==");
        assert (one ^ other) == other;
    }

    @Test
    public void and() {
        int one = 10;
        int other = 20;

        test.willCapture("one", one);
        test.willCapture("other", other);
        test.willUse("&");
        test.willUse("==");
        assert (one & other) == other;
    }

    @Test
    public void increment() {
        int one = 10;
        int other = 20;

        test.willCapture("one", one);
        test.willCapture("other", other);
        test.willUse("++");
        test.willUse("==");
        assert one++ == other;
    }

    @Test
    public void incrementPre() {
        int one = 10;
        int other = 20;

        test.willCapture("one", one + 1);
        test.willCapture("other", other);
        test.willUse("++one");
        test.willUse("==");
        assert ++one == other;
    }

    @Test
    public void decrement() {
        int one = 10;
        int other = 20;

        test.willCapture("one", one);
        test.willCapture("other", other);
        test.willUse("one--");
        test.willUse("==");
        assert one-- == other;
    }

    @Test
    public void decrementPre() {
        int one = 10;
        int other = 20;

        test.willCapture("one", one - 1);
        test.willCapture("other", other);
        test.willUse("--");
        test.willUse("==");
        assert --one == other;
    }

    @Test
    public void instanceOf() {
        Object value = "test";

        test.willCapture("value", value);
        test.willUse("instanceof");
        assert value instanceof Map;
    }

    @Test
    public void instantiate() {
        Object value = "test";

        test.willCapture("value", value);
        test.willUse("new Object()");
        assert value == new Object();
    }

    @Test
    public void instantiateWithParameter() {
        Object value = "test";

        test.willCapture("value", value);
        test.willUse("new String(\"fail\")");
        assert value == new String("fail");
    }

    @Test
    public void assertTwice() throws Exception {
        int value = 2;
        assert value != 1; // success

        test.willUse("==");
        test.willCapture("value", value);
        assert value == 3;
    }

    @Test
    public void external() {
        String value = "test";

        test.willCapture("value", value);
        test.willCapture("value.length()", 4);
        External.assertInExternal(value);
    }

    /**
     * @version 2012/01/22 19:58:35
     */
    private static class External {

        private static void assertInExternal(String value) {
            assert value.length() == 20;
        }
    }
}
