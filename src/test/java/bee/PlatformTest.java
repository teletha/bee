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

class PlatformTest {

    @Test
    void java() {
        assert Platform.Java.isPresent();
        assert Platform.JavaHome.isPresent();
        assert Platform.JavaHome.directory("bin").isPresent();
    }

    @Test
    void beeHome() {
        assert Platform.BeeHome != null;

        // The Bee home must be independent from the JDK installation. When BEE_HOME is explicitly
        // configured, its location is respected even if it points inside the JDK.
        if (System.getenv("BEE_HOME") == null) {
            String home = Platform.BeeHome.absolutize().path();
            String java = Platform.JavaHome.absolutize().path();
            assert !home.startsWith(java);
        }
    }

    @Test
    void localRepository() {
        assert Platform.BeeLocalRepository != null;
    }
}
