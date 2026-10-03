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
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import bee.AbstractTaskTest;
import bee.api.Repository;
import kiss.I;
import psychopath.Directory;
import psychopath.File;
import psychopath.Locator;

@Isolated
@Execution(ExecutionMode.SAME_THREAD)
class InstallTest extends AbstractTaskTest {

    @Test
    void installJar() {
        Repository repository = I.make(Repository.class);
        Directory local = Locator.temporaryDirectory();
        repository.setLocalRepository(local);

        // The task asks for a jar file. A single candidate is selected without any interaction.
        project.getRoot().file("sample.jar").create();

        new Install() {
        }.jar();

        // The jar is installed under the coordinates derived from its file name.
        assert local.file("sample/jar/sample.jar/1.0/sample.jar-1.0.jar").isPresent();
        assert local.file("sample/jar/sample.jar/1.0/sample.jar-1.0.pom").isPresent();
    }

    @Test
    void installProject() {
        Repository repository = I.make(Repository.class);
        Directory local = Locator.temporaryDirectory();
        repository.setLocalRepository(local);

        // Install the current project directly (without running the test/jar sub tasks).
        File jar = Locator.temporaryFile().text("dummy");
        repository.install(project, jar);

        assert local.file("test/test/1.0/test-1.0.jar").isPresent();
        assert local.file("test/test/1.0/test-1.0.pom").isPresent();
    }
}
