/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
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
}
