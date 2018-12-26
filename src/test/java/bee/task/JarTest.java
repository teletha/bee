/*
 * Copyright (C) 2018 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import bee.BlinkProject;

class JarTest {

    @Test
    void jarMainSource() throws IOException {
        BlinkProject project = new BlinkProject();
        project.source("A");
        project.source("test.B");
        project.resource("C");

        Path A = project.locateJar();

        assert Files.notExists(A);

        Jar jar = new Jar();
        jar.source();

        assert Files.exists(A);
        assert Files.size(A) != 0;
    }
}
