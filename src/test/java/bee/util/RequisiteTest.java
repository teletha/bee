/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.util;

import org.junit.jupiter.api.Test;

class RequisiteTest {

    @Test
    void testName() {
        Ensure<CharSequence> req = Ensure.Text.length(3, 7);
        assert req.test("min");
        assert req.test("maxchar");
        assert req.test("no") == false;
        assert req.test("over character") == false;
    }

    @Test
    void pattern() {
        String pattern = "AaN.";
        Ensure<CharSequence> req = Ensure.Text.pattern("\\p{Alpha}[\\w\\-]*(\\.\\p{Alpha}[\\w\\-]*)*");
        assert req.test("ok");
        assert req.test("ok.test");
        assert req.test(".") == false;
        assert req.test("no.") == false;
        assert req.test("@mark") == false;
        assert req.test("1stnum") == false;
    }
}
