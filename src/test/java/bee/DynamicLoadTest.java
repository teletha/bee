/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import kiss.I;
import psychopath.Locator;

class DynamicLoadTest {

    @Test
    void load() {
        String fqcn = "com.github.teletha.AB";

        Assertions.assertThrows(ClassNotFoundException.class, () -> I.type(fqcn));
        BeeLoader.load(Locator.file("src/test/resources/dynamic-load.jar.file"));
        assert I.type(fqcn) != null;
    }
}
