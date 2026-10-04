/*
 * Copyright (C) 2026 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.api;

import org.junit.jupiter.api.Test;

class JavaVersionTest {

    @Test
    void feature() {
        assert JavaVersion.JAVA_8.feature == 8;
        assert JavaVersion.JAVA_21.feature == 21;
    }

    @Test
    void lts() {
        assert JavaVersion.JAVA_8.lts;
        assert JavaVersion.JAVA_11.lts;
        assert JavaVersion.JAVA_17.lts;
        assert JavaVersion.JAVA_21.lts;
        assert JavaVersion.JAVA_25.lts;
        assert !JavaVersion.JAVA_24.lts;
        assert !JavaVersion.JAVA_23.lts;
    }

    @Test
    void release() {
        assert JavaVersion.JAVA_21.release != null;
        assert JavaVersion.JAVA_8.release.isBefore(JavaVersion.JAVA_21.release);
        assert JavaVersion.JAVA_21.getReleaseDate().matches("\\d{4}/\\d{2}/\\d{2}");
    }

    @Test
    void of() {
        assert JavaVersion.of(21) == JavaVersion.JAVA_21;
        assert JavaVersion.of(8) == JavaVersion.JAVA_8;
        assert JavaVersion.of(-1) == null;
    }

    @Test
    void parse() {
        assert JavaVersion.parse("21") == JavaVersion.JAVA_21;
        assert JavaVersion.parse("1.8") == JavaVersion.JAVA_8;
        assert JavaVersion.parse("Java 17") == JavaVersion.JAVA_17;
        assert JavaVersion.parse(null) == null;
        assert JavaVersion.parse("abc") == null;
    }

    @Test
    void latest() {
        assert JavaVersion.latest() == JavaVersion.values()[JavaVersion.values().length - 1];
        assert JavaVersion.latest().feature >= 27;
    }

    @Test
    void current() {
        assert JavaVersion.current() != null;
        assert JavaVersion.current().feature == Runtime.version().feature();
    }

    @Test
    void ltsList() {
        assert JavaVersion.lts().length == 5;
        for (JavaVersion version : JavaVersion.lts()) {
            assert version.lts;
        }
    }
}
