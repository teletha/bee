/*
 * Copyright (C) 2023 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.api;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import antibug.CleanRoom;
import bee.BlinkProject;
import bee.UserInterface;
import psychopath.Locator;

@Disabled
class DownloadTest {

    @RegisterExtension
    private static CleanRoom room = new CleanRoom();

    @Test
    void testName() {
        BlinkProject project = new BlinkProject(UserInterface.CUI);
        // project.require("org.openjfx", "javafx-graphics", "18-ea+9");
        // project.require("org.openjfx", "javafx-web", "18-ea+9");
        project.require("org.apache.maven", "maven-core", "3.8.4");
        // project.require("org.springframework", "spring-orm", "5.3.15");
        // project.require("org.apache.camel", "camel-core", "3.14.0");

        Repository repository = new Repository(project);
        repository.setLocalRepository(Locator.directory(room.root));

        repository.collectDependency(project, Scope.Compile);
    }
}