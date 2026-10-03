/*
 * Copyright (C) 2026 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.util;

import org.junit.jupiter.api.Test;

class ProcessTest {

    @Test
    void isAvailable() {
        assert Process.isAvailable("java");
    }

    @Test
    void run() {
        assert Process.with().ignoreOutput().run("java", "-version") == 0;
    }

    @Test
    void read() {
        assert Process.with().read("java", "-version").contains("version");
    }
}
