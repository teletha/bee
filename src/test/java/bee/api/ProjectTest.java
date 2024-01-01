/*
 * Copyright (C) 2024 The BEE Development Team
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

import bee.BlinkProject;
import bee.Fail;

class ProjectTest {

    @Test
    void groupNull() {
        BlinkProject project = new BlinkProject();

        Assertions.assertThrows(Fail.class, () -> project.product(null, "PRODUCT", "1.5"));
    }

    @Test
    void associate() {
        BlinkProject project = new BlinkProject();

        Key key = project.associate(Key.class);
        assert key != null;

        Key same = project.associate(Key.class);
        assert key == same;
    }

    private static class Key {
    }

    @Test
    void associateNull() {
        BlinkProject project = new BlinkProject();
        Assertions.assertThrows(NullPointerException.class, () -> project.associate(null));
    }
}