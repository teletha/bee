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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import antibug.CleanRoom;
import bee.BlinkProject;
import psychopath.Locator;

class LibraryTest {
    @RegisterExtension
    private static CleanRoom room = new CleanRoom();

    private BlinkProject project;

    private Repository repository;

    @BeforeEach
    void setup() {
        project = new BlinkProject();
        repository = new Repository(project);
        repository.setLocalRepository(Locator.directory(room.locateRadom()));
    }

    @Test
    void source() {
        project.require("org.apache.maven.resolver", "maven-resolver-api", "1.3.1");

        Library library = repository.collectDependency(project, Scope.Compile).stream().findFirst().orElseThrow();
        assert repository.resolveSource(library).isPresent();
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void sourceWithClassfier() {
        project.require("org.openjfx", "javafx-base", "11");

        Library library = repository.collectDependency(project, Scope.Compile)
                .stream()
                .filter(l -> !l.classfier.isEmpty())
                .findFirst()
                .orElseThrow();

        assert repository.resolveSource(library).isPresent();
    }

    @Test
    void javadoc() {
        project.require("org.apache.maven.resolver", "maven-resolver-api", "1.3.1");

        Library library = repository.collectDependency(project, Scope.Compile).stream().findFirst().orElseThrow();

        assert repository.resolveJavadoc(library).isPresent();
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void javadocWithClassfier() {
        project.require("org.openjfx", "javafx-base", "11");

        Library library = repository.collectDependency(project, Scope.Compile)
                .stream()
                .filter(l -> !l.classfier.isEmpty())
                .findFirst()
                .orElseThrow();

        assert repository.resolveJavadoc(library).isPresent();
    }
}