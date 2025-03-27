/*
 * Copyright (C) 2024 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import kiss.I;
import psychopath.Locator;

class PriorityClassLoaderTest {

    private PriorityClassLoader create() {
        return new PriorityClassLoader(UserInterface.CUI);
    }

    @Test
    void load() throws ClassNotFoundException {
        String fqcn = "com.github.teletha.AB";
        Assertions.assertThrows(ClassNotFoundException.class, () -> I.type(fqcn));

        PriorityClassLoader loader = create().addClassPath(Locator.file("src/test/resources/dynamic-load.jar.file"));
        assert Class.forName(fqcn, true, loader) != null;
    }
}