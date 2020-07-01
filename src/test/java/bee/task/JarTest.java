/*
 * Copyright (C) 2020 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import bee.BlinkProject;
import psychopath.File;

class JarTest {

    @Test
    void jarMainSource() throws IOException {
        BlinkProject project = new BlinkProject();
        project.source("A");
        project.source("test.B");
        project.resource("C");

        File A = project.locateJar();

        assert A.isAbsent();

        Jar jar = new Jar();
        jar.source();

        assert A.isPresent();
        assert A.size() != 0;
    }
}