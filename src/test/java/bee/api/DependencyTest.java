/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.api;

import java.util.Set;

import org.junit.jupiter.api.Test;

import bee.BlinkProject;

class DependencyTest {

    @Test
    void empty() {
        BlinkProject project = new BlinkProject();
        Repository repository = new Repository(project);
        Set<Library> dependencies = repository.collectDependency(project, Scope.Compile);
        assert dependencies.size() == 0;
    }

    @Test
    void external() {
        BlinkProject project = new BlinkProject();
        project.require("org.ow2.asm", "asm", "5.0");

        Repository repository = new Repository(project);
        Set<Library> dependencies = repository.collectDependency(project, Scope.Compile);
        assert dependencies.size() == 1;
    }

    @Test
    void atTest1() {
        BlinkProject project = new BlinkProject();
        project.require("org.ow2.asm", "asm", "5.0.4").atTest();

        Repository repository = new Repository(project);
        Set<Library> dependencies = repository.collectDependency(project, Scope.Test);
        assert dependencies.size() == 1;
        dependencies = repository.collectDependency(project, Scope.Runtime);
        assert dependencies.size() == 0;
    }

    @Test
    void atTest2() {
        BlinkProject project = new BlinkProject();
        project.require("org.ow2.asm", "asm-tree", "5.0.4").atTest();

        Repository repository = new Repository(project);
        Set<Library> dependencies = repository.collectDependency(project, Scope.Test);
        assert dependencies.size() == 2;
        dependencies = repository.collectDependency(project, Scope.Runtime);
        assert dependencies.size() == 0;
    }

    @Test
    void atAnnotation() {
        BlinkProject project = new BlinkProject();
        project.require("org.atteo.classindex", "classindex", "3.4").atAnnotation();

        Repository repository = new Repository(project);
        Set<Library> dependencies = repository.collectDependency(project, Scope.Annotation);
        assert dependencies.size() == 1;
        dependencies = repository.collectDependency(project, Scope.Runtime);
        assert dependencies.size() == 0;
    }

    @Test
    void external1() {
        BlinkProject project = new BlinkProject();
        project.require("org.skyscreamer", "jsonassert", "1.2.3");

        Repository repository = new Repository(project);
        Set<Library> dependencies = repository.collectDependency(project, Scope.Runtime);
        assert dependencies.size() == 2;
        dependencies = repository.collectDependency(project, Scope.Test, Scope.Compile);
        assert dependencies.size() == 2;
    }

    @Test
    void byLibrary() {
        Library library = new Library("org.skyscreamer", "jsonassert", "1.2.3");
        Repository repo = new Repository(new BlinkProject());
        Set<Library> dep = repo.collectDependency(library, Scope.Runtime);
        assert dep.size() == 2;

        dep = repo.collectDependency(library, Scope.Test, Scope.Compile);
        assert dep.size() == 2;
    }

    @Test
    void byLibraryWithClassifier() {
        Library library = new Library("org.bytedeco", "javacv-platform", "1.3.1");
        Repository repo = new Repository(new BlinkProject());
        Set<Library> dep = repo.collectDependency(library, Scope.Test, Scope.Compile);
        assert dep.size() == 84;
    }
}