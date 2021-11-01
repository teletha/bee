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

import java.util.function.Consumer;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import antibug.CleanRoom;
import bee.BlinkProject;
import psychopath.Locator;

class DependencyTest {

    @RegisterExtension
    private static CleanRoom room = new CleanRoom();

    private static Repository repository;

    @BeforeAll
    static void setup() {
        repository = new Repository(new BlinkProject());
        repository.setLocalRepository(Locator.directory(room.root));
    }

    @Test
    void empty() {
        BlinkProject project = new BlinkProject();
        Repository repository = new Repository(project);
        assert repository.collectDependency(project, Scope.Compile).size() == 0;
    }

    @Test
    void atCompile() {
        BlinkProject project = new BlinkProject();
        project.require("org.ow2.asm", "asm", "9.2");

        Repository repository = new Repository(project);
        assert repository.collectDependency(project, Scope.Annotation).size() == 0;
        assert repository.collectDependency(project, Scope.Compile).size() == 1;
        assert repository.collectDependency(project, Scope.Provided).size() == 0;
        assert repository.collectDependency(project, Scope.Runtime).size() == 1;
        assert repository.collectDependency(project, Scope.Test).size() == 0;
        assert repository.collectDependency(project, Scope.System).size() == 0;
    }

    @Test
    void atTest1() {
        BlinkProject project = new BlinkProject();
        project.require("org.ow2.asm", "asm", "9.2").atTest();

        Repository repository = new Repository(project);
        assert repository.collectDependency(project, Scope.Annotation).size() == 0;
        assert repository.collectDependency(project, Scope.Compile).size() == 0;
        assert repository.collectDependency(project, Scope.Provided).size() == 0;
        assert repository.collectDependency(project, Scope.Runtime).size() == 0;
        assert repository.collectDependency(project, Scope.Test).size() == 1;
        assert repository.collectDependency(project, Scope.System).size() == 0;
    }

    @Test
    void atTest2() {
        BlinkProject project = new BlinkProject();
        project.require("org.ow2.asm", "asm-tree", "5.0.4").atTest();

        Repository repository = new Repository(project);
        assert repository.collectDependency(project, Scope.Test).size() == 2;
        assert repository.collectDependency(project, Scope.Runtime).size() == 0;
    }

    @Test
    void atAnnotation() {
        BlinkProject project = new BlinkProject();
        project.require("org.atteo.classindex", "classindex", "3.4").atAnnotation();

        Repository repository = new Repository(project);
        assert repository.collectDependency(project, Scope.Annotation).size() == 1;
        assert repository.collectDependency(project, Scope.Runtime).size() == 0;
    }

    @Test
    void atProvided1() {
        BlinkProject project = new BlinkProject();
        project.require("org.ow2.asm", "asm", "9.2").atProvided();

        Repository repository = new Repository(project);
        assert repository.collectDependency(project, Scope.Compile).size() == 1;
        assert repository.collectDependency(project, Scope.Provided).size() == 1;
        assert repository.collectDependency(project, Scope.Runtime).size() == 0;
    }

    @Test
    void atProvided2() {
        BlinkProject project = new BlinkProject();
        project.require("org.ow2.asm", "asm", "9.2");
        project.require("org.ow2.asm", "asm-util", "9.2").atProvided();

        Repository repository = new Repository(project);
        assert repository.collectDependency(project, Scope.Compile).size() == 4;
        assert repository.collectDependency(project, Scope.Provided).size() == 4;
        assert repository.collectDependency(project, Scope.Runtime).size() == 1;
    }

    @Test
    void external1() {
        BlinkProject project = new BlinkProject();
        project.require("org.skyscreamer", "jsonassert", "1.2.3");

        Repository repository = new Repository(project);
        assert repository.collectDependency(project, Scope.Runtime).size() == 2;
        assert repository.collectDependency(project, Scope.Test, Scope.Compile).size() == 2;
    }

    @Test
    void byLibrary() {
        Library library = new Library("org.skyscreamer", "jsonassert", "1.2.3");
        Repository repo = new Repository(new BlinkProject());
        assert repo.collectDependency(library, Scope.Runtime).size() == 2;
        assert repo.collectDependency(library, Scope.Test, Scope.Compile).size() == 2;
    }

    @Test
    void byLibraryWithClassifier() {
        Library library = new Library("org.bytedeco", "javacv-platform", "1.3.1");
        Repository repo = new Repository(new BlinkProject());
        assert repo.collectDependency(library, Scope.Test, Scope.Compile).size() == 84;
    }

    @Test
    void compile_compile() {
        TemporaryProject project = new TemporaryProject();
        project.require("one", one -> {
            one.require("nest");
        });

        assert repository.collectDependency(project, Scope.Annotation).size() == 0;
        assert repository.collectDependency(project, Scope.Compile).size() == 2;
        assert repository.collectDependency(project, Scope.Provided).size() == 0;
        assert repository.collectDependency(project, Scope.Runtime).size() == 2;
        assert repository.collectDependency(project, Scope.Test).size() == 0;
        assert repository.collectDependency(project, Scope.System).size() == 0;
    }

    @Test
    void compile_test() {
        TemporaryProject project = new TemporaryProject();
        project.require("one", one -> {
            one.require("nest").atTest();
        });

        assert repository.collectDependency(project, Scope.Annotation).size() == 0;
        assert repository.collectDependency(project, Scope.Compile).size() == 1;
        assert repository.collectDependency(project, Scope.Provided).size() == 0;
        assert repository.collectDependency(project, Scope.Runtime).size() == 1;
        assert repository.collectDependency(project, Scope.Test).size() == 0;
        assert repository.collectDependency(project, Scope.System).size() == 0;
    }

    /**
     * 
     */
    private static class TemporaryProject extends BlinkProject {

        /**
         * 
         */
        private TemporaryProject() {
            this(RandomStringUtils.randomAlphabetic(7));
        }

        /**
         * 
         */
        private TemporaryProject(String name) {
            product("temporary.test.project", name, "1.0");

            setOutput(Locator.temporaryDirectory());
            locateJar().create();
        }

        private Library require(String productName) {
            return require(productName, noop -> {
            });
        }

        private Library require(String productName, Consumer<TemporaryProject> definition) {
            TemporaryProject project = new TemporaryProject(getProduct() + "-" + productName);

            definition.accept(project);
            repository.install(project);

            return require(project.getGroup(), project.getProduct(), project.getVersion());
        }
    }

}