/*
 * Copyright (C) 2025 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import bee.TestableProject;

class ProjectTest {

    @Test
    void groupNull() {
        TestableProject project = new TestableProject();

        Assertions.assertThrows(NullPointerException.class, () -> project.product(null, "PRODUCT", "1.5"));
    }

    @Test
    void associate() {
        TestableProject project = new TestableProject();

        Key key = project.associate(Key.class);
        assert key != null;

        Key same = project.associate(Key.class);
        assert key == same;
    }

    private static class Key {
    }

    @Test
    void associateNull() {
        TestableProject project = new TestableProject();
        Assertions.assertThrows(NullPointerException.class, () -> project.associate(null));
    }
}