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

public class CharacterTest {

    @Rule
    public static final PowerAssertTester tester = new PowerAssertTester();

    @Test
    public void constant() throws Exception {
        char value = 'a';

        tester.willUse("'b'");
        tester.willCapture("value", value);
        assert 'b' == value;
    }

    @Test
    public void array() throws Exception {
        char[] array = {0, 1, 2};

        tester.willCapture("array", array);
        assert array == null;
    }

    @Test
    public void arrayIndex() throws Exception {
        char[] array = {'a', '1', 'あ'};

        tester.willCapture("array", array);
        tester.willCapture("array[1]", '1');
        tester.willUse("'@'");
        assert array[1] == '@';
    }

    @Test
    public void arrayLength() throws Exception {
        char[] array = {'a', '1', 'あ'};

        tester.willCapture("array", array);
        tester.willCapture("array.length", 3);
        assert array.length == 10;
    }

    @Test
    public void arrayNew() throws Exception {
        tester.willUse("new char[] {'a', '1'} == null");
        assert new char[] {'a', '1'} == null;
    }

    @Test
    public void varargs() throws Exception {
        tester.willCapture("var()", false);
        assert var();
    }

    boolean var(char... var) {
        return false;
    }

    @Test
    public void varargsWithHead() throws Exception {
        tester.willCapture("head('c')", false);
        assert head('c');
    }

    boolean head(char head, char... var) {
        return false;
    }

    @Test
    public void method() throws Exception {
        tester.willCapture("test()", 'r');
        tester.willUse("'a'");
        assert test() == 'a';
    }

    char test() {
        return 'r';
    }

    @Test
    public void parameter() throws Exception {
        tester.willCapture("test('p')", false);
        assert test('p');
    }

    private boolean test(char value) {
        return false;
    }

    /** The tester. */
    private char charField = 'a';

    /** The tester. */
    private static char charFieldStatic = 'a';

    @Test
    public void fieldCharacterAccess() throws Exception {
        tester.willCapture("charField", 'a');
        tester.willUse("'b'");
        assert charField == 'b';
    }

    @Test
    public void fieldIntAccessWithHiddenName() throws Exception {
        char charField = 'a';

        tester.willCapture("this.charField", charField);
        assert this.charField == 0;
    }

    @Test
    public void fieldCharacterStaticAccess() throws Exception {
        tester.willCapture("charFieldStatic", 'a');
        tester.willUse("'b'");
        assert charFieldStatic == 'b';
    }
}
