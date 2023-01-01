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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import bee.BlinkProject;

/**
 * 
 */
class LibraryTest {

    @Test
    void source() {
        BlinkProject project = new BlinkProject();
        project.require("org.apache.maven.resolver", "maven-resolver-api", "1.3.1");

        Repository repository = new Repository(project);
        Library library = repository.collectDependency(project, Scope.Compile).stream().findFirst().orElseThrow();

        assert repository.resolveSource(library).isPresent();
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void sourceWithClassfier() {
        BlinkProject project = new BlinkProject();
        project.require("org.openjfx", "javafx-base", "11");

        Repository repository = new Repository(project);
        Library library = repository.collectDependency(project, Scope.Compile)
                .stream()
                .filter(l -> !l.classfier.isEmpty())
                .findFirst()
                .orElseThrow();

        assert repository.resolveSource(library).isPresent();
    }

    @Test
    void javadoc() {
        BlinkProject project = new BlinkProject();
        project.require("org.apache.maven.resolver", "maven-resolver-api", "1.3.1");

        Repository repository = new Repository(project);
        Library library = repository.collectDependency(project, Scope.Compile).stream().findFirst().orElseThrow();

        assert repository.resolveJavadoc(library).isPresent();
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void javadocWithClassfier() {
        BlinkProject project = new BlinkProject();
        project.require("org.openjfx", "javafx-base", "11");

        Repository repository = new Repository(project);
        Library library = repository.collectDependency(project, Scope.Compile)
                .stream()
                .filter(l -> !l.classfier.isEmpty())
                .findFirst()
                .orElseThrow();

        assert repository.resolveJavadoc(library).isPresent();
    }
}