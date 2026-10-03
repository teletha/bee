/*
 * Copyright (C) 2026 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import org.junit.jupiter.api.Test;

import bee.AbstractTaskTest;
import bee.Task;
import psychopath.File;

class MavenTest extends AbstractTaskTest {

    @Test
    void generatePom() {
        File pom = project.getRoot().file("pom.xml");
        assert pom.isAbsent();

        Task.by(Maven.class).pom();

        assert pom.isPresent();

        String text = pom.text();
        assert text.contains("<groupId>test</groupId>");
        assert text.contains("<artifactId>test</artifactId>");
        assert text.contains("<version>1.0</version>");
    }
}
