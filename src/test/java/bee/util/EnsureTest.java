/*
 * Copyright (C) 2025 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.util;

import org.junit.jupiter.api.Test;

class EnsureTest {

    @Test
    void alphabetic() {
        Ensure<CharSequence> req = Ensure.Alphabetic;
        assert req.test("ok");
        assert req.test("a");
        assert req.test("z");
        assert req.test("A");
        assert req.test("Z");
        assert req.test("ë");
        assert req.test("Ⅳ");
        assert req.test("+") == false;
        assert req.test("_") == false;
        assert req.test("*") == false;
        assert req.test("0") == false;
        assert req.test("あ") == false;
        assert req.test(" ") == false;
        assert req.test("漢") == false;
        assert req.test("ア") == false;
        assert req.test("ｱ") == false;
        assert req.test("四") == false;
    }

    @Test
    void size() {
        Ensure<CharSequence> req = Ensure.Text.size(3, 7);
        assert req.test("min");
        assert req.test("maxchar");
        assert req.test("no") == false;
        assert req.test("over character") == false;
    }

    @Test
    void separator() {
        Ensure<CharSequence> req = Ensure.Alphabetic.separator("+-");
        assert req.test("ok");
        assert req.test("ok+ok");
        assert req.test("ok-ok");
        assert req.test("unknown*separator") == false;
        assert req.test("+dont+use+at+head") == false;
        assert req.test("dont+use+at+tail+") == false;
        assert req.test("invalid++sequencial+usage") == false;
    }

    @Test
    void pattern() {
        Ensure<CharSequence> req = Ensure.Text.pattern("\\p{Alpha}[\\w\\-]*(\\.\\p{Alpha}[\\w\\-]*)*");
        assert req.test("ok");
        assert req.test("ok.test");
        assert req.test(".") == false;
        assert req.test("no.") == false;
        assert req.test("@mark") == false;
        assert req.test("1stnum") == false;
    }
}