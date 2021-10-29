/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee;

import org.apache.commons.lang3.RandomStringUtils;

public class RandomProject extends BlinkProject {

    /**
     * 
     */
    public RandomProject() {
        product("random.test.project", RandomStringUtils.randomAlphabetic(20), "1.0");
    }
}
