/*
 * Copyright (C) 2026 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee;

import org.junit.jupiter.api.Test;

class FailTest {

    @Test
    void message() {
        Fail fail = new Fail("something wrong");
        assert fail.getMessage().equals("something wrong");
    }

    @Test
    void solve() {
        Fail fail = new Fail("reason").solve("try this").solve("or that");
        String message = fail.getMessage();

        assert message.contains("reason");
        assert message.contains("try this");
        assert message.contains("or that");
    }

    @Test
    void ignoreNullSolution() {
        Fail fail = new Fail("reason").solve(null);
        assert fail.getMessage().equals("reason");
    }

    @Test
    void cause() {
        Exception cause = new Exception("root");
        Fail fail = new Fail("reason").reason(cause);

        assert fail.getCause() == cause;
    }
}
