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

    @Test
    void compile_annotation() {
        TemporaryProject project = new TemporaryProject();
        project.require("one", one -> {
            one.require("nest").atAnnotation();
        });

        assert repository.collectDependency(project, Scope.Annotation).size() == 0;
        assert repository.collectDependency(project, Scope.Compile).size() == 1;
        assert repository.collectDependency(project, Scope.Provided).size() == 0;
        assert repository.collectDependency(project, Scope.Runtime).size() == 1;
        assert repository.collectDependency(project, Scope.Test).size() == 0;
        assert repository.collectDependency(project, Scope.System).size() == 0;
    }

    @Test
    void compile_provided() {
        TemporaryProject project = new TemporaryProject();
        project.require("one", one -> {
            one.require("nest").atProvided();
        });

        assert repository.collectDependency(project, Scope.Annotation).size() == 0;
        assert repository.collectDependency(project, Scope.Compile).size() == 1;
        assert repository.collectDependency(project, Scope.Provided).size() == 0;
        assert repository.collectDependency(project, Scope.Runtime).size() == 1;
        assert repository.collectDependency(project, Scope.Test).size() == 0;
        assert repository.collectDependency(project, Scope.System).size() == 0;
    }

    @Test
    void compile_system() {
        TemporaryProject project = new TemporaryProject();
        project.require("one", one -> {
            one.require("nest").atSystem();
        });

        assert repository.collectDependency(project, Scope.Annotation).size() == 0;
        assert repository.collectDependency(project, Scope.Compile).size() == 1;
        assert repository.collectDependency(project, Scope.Provided).size() == 0;
        assert repository.collectDependency(project, Scope.Runtime).size() == 1;
        assert repository.collectDependency(project, Scope.Test).size() == 0;
        assert repository.collectDependency(project, Scope.System).size() == 0;
    }

    @Test
    void compile_runtime() {
        TemporaryProject project = new TemporaryProject();
        project.require("one", one -> {
            one.require("nest").atRuntime();
        });

        assert repository.collectDependency(project, Scope.Annotation).size() == 0;
        assert repository.collectDependency(project, Scope.Compile).size() == 1;
        assert repository.collectDependency(project, Scope.Provided).size() == 0;
        assert repository.collectDependency(project, Scope.Runtime).size() == 2;
        assert repository.collectDependency(project, Scope.Test).size() == 0;
        assert repository.collectDependency(project, Scope.System).size() == 0;
    }

    @Test
    void test_compile() {
        TemporaryProject project = new TemporaryProject();
        project.require("one", one -> {
            one.require("nest");
        }).atTest();

        assert repository.collectDependency(project, Scope.Annotation).size() == 0;
        assert repository.collectDependency(project, Scope.Compile).size() == 0;
        assert repository.collectDependency(project, Scope.Provided).size() == 0;
        assert repository.collectDependency(project, Scope.Runtime).size() == 0;
        assert repository.collectDependency(project, Scope.Test).size() == 2;
        assert repository.collectDependency(project, Scope.System).size() == 0;
    }

    @Test
    void test_test() {
        TemporaryProject project = new TemporaryProject();
        project.require("one", one -> {
            one.require("nest").atTest();
        }).atTest();

        assert repository.collectDependency(project, Scope.Annotation).size() == 0;
        assert repository.collectDependency(project, Scope.Compile).size() == 0;
        assert repository.collectDependency(project, Scope.Provided).size() == 0;
        assert repository.collectDependency(project, Scope.Runtime).size() == 0;
        assert repository.collectDependency(project, Scope.Test).size() == 1;
        assert repository.collectDependency(project, Scope.System).size() == 0;
    }

    @Test
    void test_annotation() {
        TemporaryProject project = new TemporaryProject();
        project.require("one", one -> {
            one.require("nest").atAnnotation();
        }).atTest();

        assert repository.collectDependency(project, Scope.Annotation).size() == 0;
        assert repository.collectDependency(project, Scope.Compile).size() == 0;
        assert repository.collectDependency(project, Scope.Provided).size() == 0;
        assert repository.collectDependency(project, Scope.Runtime).size() == 0;
        assert repository.collectDependency(project, Scope.Test).size() == 1;
        assert repository.collectDependency(project, Scope.System).size() == 0;
    }

    @Test
    void test_provided() {
        TemporaryProject project = new TemporaryProject();
        project.require("one", one -> {
            one.require("nest").atProvided();
        }).atTest();

        assert repository.collectDependency(project, Scope.Annotation).size() == 0;
        assert repository.collectDependency(project, Scope.Compile).size() == 0;
        assert repository.collectDependency(project, Scope.Provided).size() == 0;
        assert repository.collectDependency(project, Scope.Runtime).size() == 0;
        assert repository.collectDependency(project, Scope.Test).size() == 1;
        assert repository.collectDependency(project, Scope.System).size() == 0;
    }

    @Test
    void test_system() {
        TemporaryProject project = new TemporaryProject();
        project.require("one", one -> {
            one.require("nest").atSystem();
        }).atTest();

        assert repository.collectDependency(project, Scope.Annotation).size() == 0;
        assert repository.collectDependency(project, Scope.Compile).size() == 0;
        assert repository.collectDependency(project, Scope.Provided).size() == 0;
        assert repository.collectDependency(project, Scope.Runtime).size() == 0;
        assert repository.collectDependency(project, Scope.Test).size() == 1;
        assert repository.collectDependency(project, Scope.System).size() == 0;
    }

    @Test
    void test_runtime() {
        TemporaryProject project = new TemporaryProject();
        project.require("one", one -> {
            one.require("nest").atRuntime();
        }).atTest();

        assert repository.collectDependency(project, Scope.Annotation).size() == 0;
        assert repository.collectDependency(project, Scope.Compile).size() == 0;
        assert repository.collectDependency(project, Scope.Provided).size() == 0;
        assert repository.collectDependency(project, Scope.Runtime).size() == 0;
        assert repository.collectDependency(project, Scope.Test).size() == 2;
        assert repository.collectDependency(project, Scope.System).size() == 0;
    }

    @Test
    void provided_compile() {
        TemporaryProject project = new TemporaryProject();
        project.require("one", one -> {
            one.require("nest");
        }).atProvided();

        assert repository.collectDependency(project, Scope.Annotation).size() == 0;
        assert repository.collectDependency(project, Scope.Compile).size() == 2;
        assert repository.collectDependency(project, Scope.Provided).size() == 2;
        assert repository.collectDependency(project, Scope.Runtime).size() == 0;
        assert repository.collectDependency(project, Scope.Test).size() == 0;
        assert repository.collectDependency(project, Scope.System).size() == 0;
    }

    @Test
    void provided_test() {
        TemporaryProject project = new TemporaryProject();
        project.require("one", one -> {
            one.require("nest").atTest();
        }).atProvided();

        assert repository.collectDependency(project, Scope.Annotation).size() == 0;
        assert repository.collectDependency(project, Scope.Compile).size() == 1;
        assert repository.collectDependency(project, Scope.Provided).size() == 1;
        assert repository.collectDependency(project, Scope.Runtime).size() == 0;
        assert repository.collectDependency(project, Scope.Test).size() == 0;
        assert repository.collectDependency(project, Scope.System).size() == 0;
    }

    @Test
    void provided_annotation() {
        TemporaryProject project = new TemporaryProject();
        project.require("one", one -> {
            one.require("nest").atAnnotation();
        }).atProvided();

        assert repository.collectDependency(project, Scope.Annotation).size() == 0;
        assert repository.collectDependency(project, Scope.Compile).size() == 1;
        assert repository.collectDependency(project, Scope.Provided).size() == 1;
        assert repository.collectDependency(project, Scope.Runtime).size() == 0;
        assert repository.collectDependency(project, Scope.Test).size() == 0;
        assert repository.collectDependency(project, Scope.System).size() == 0;
    }

    @Test
    void provided_provided() {
        TemporaryProject project = new TemporaryProject();
        project.require("one", one -> {
            one.require("nest").atProvided();
        }).atProvided();

        assert repository.collectDependency(project, Scope.Annotation).size() == 0;
        assert repository.collectDependency(project, Scope.Compile).size() == 1;
        assert repository.collectDependency(project, Scope.Provided).size() == 1;
        assert repository.collectDependency(project, Scope.Runtime).size() == 0;
        assert repository.collectDependency(project, Scope.Test).size() == 0;
        assert repository.collectDependency(project, Scope.System).size() == 0;
    }

    @Test
    void provided_system() {
        TemporaryProject project = new TemporaryProject();
        project.require("one", one -> {
            one.require("nest").atSystem();
        }).atProvided();

        assert repository.collectDependency(project, Scope.Annotation).size() == 0;
        assert repository.collectDependency(project, Scope.Compile).size() == 1;
        assert repository.collectDependency(project, Scope.Provided).size() == 1;
        assert repository.collectDependency(project, Scope.Runtime).size() == 0;
        assert repository.collectDependency(project, Scope.Test).size() == 0;
        assert repository.collectDependency(project, Scope.System).size() == 0;
    }

    @Test
    void provided_runtime() {
        TemporaryProject project = new TemporaryProject();
        project.require("one", one -> {
            one.require("nest").atRuntime();
        }).atProvided();

        assert repository.collectDependency(project, Scope.Annotation).size() == 0;
        assert repository.collectDependency(project, Scope.Compile).size() == 2;
        assert repository.collectDependency(project, Scope.Provided).size() == 2;
        assert repository.collectDependency(project, Scope.Runtime).size() == 0;
        assert repository.collectDependency(project, Scope.Test).size() == 0;
        assert repository.collectDependency(project, Scope.System).size() == 0;
    }

    @Test
    void providedDontHideItsSiblings() {
        TemporaryProject project = new TemporaryProject();
        project.require("sibling");
        project.require("provided", provided -> {
            provided.require("sibling");
        }).atProvided();

        assert repository.collectDependency(project, Scope.Annotation).size() == 0;
        assert repository.collectDependency(project, Scope.Compile).size() == 2;
        assert repository.collectDependency(project, Scope.Provided).size() == 2;
        assert repository.collectDependency(project, Scope.Runtime).size() == 1;
        assert repository.collectDependency(project, Scope.Test).size() == 0;
        assert repository.collectDependency(project, Scope.System).size() == 0;
    }

    @Test
    void runtime_compile() {
        TemporaryProject project = new TemporaryProject();
        project.require("one", one -> {
            one.require("nest");
        }).atRuntime();

        assert repository.collectDependency(project, Scope.Annotation).size() == 0;
        assert repository.collectDependency(project, Scope.Compile).size() == 0;
        assert repository.collectDependency(project, Scope.Provided).size() == 0;
        assert repository.collectDependency(project, Scope.Runtime).size() == 2;
        assert repository.collectDependency(project, Scope.Test).size() == 0;
        assert repository.collectDependency(project, Scope.System).size() == 0;
    }

    @Test
    void runtime_test() {
        TemporaryProject project = new TemporaryProject();
        project.require("one", one -> {
            one.require("nest").atTest();
        }).atRuntime();

        assert repository.collectDependency(project, Scope.Annotation).size() == 0;
        assert repository.collectDependency(project, Scope.Compile).size() == 0;
        assert repository.collectDependency(project, Scope.Provided).size() == 0;
        assert repository.collectDependency(project, Scope.Runtime).size() == 1;
        assert repository.collectDependency(project, Scope.Test).size() == 0;
        assert repository.collectDependency(project, Scope.System).size() == 0;
    }

    @Test
    void runtime_annotation() {
        TemporaryProject project = new TemporaryProject();
        project.require("one", one -> {
            one.require("nest").atAnnotation();
        }).atRuntime();

        assert repository.collectDependency(project, Scope.Annotation).size() == 0;
        assert repository.collectDependency(project, Scope.Compile).size() == 0;
        assert repository.collectDependency(project, Scope.Provided).size() == 0;
        assert repository.collectDependency(project, Scope.Runtime).size() == 1;
        assert repository.collectDependency(project, Scope.Test).size() == 0;
        assert repository.collectDependency(project, Scope.System).size() == 0;
    }

    @Test
    void runtime_provided() {
        TemporaryProject project = new TemporaryProject();
        project.require("one", one -> {
            one.require("nest").atProvided();
        }).atRuntime();

        assert repository.collectDependency(project, Scope.Annotation).size() == 0;
        assert repository.collectDependency(project, Scope.Compile).size() == 0;
        assert repository.collectDependency(project, Scope.Provided).size() == 0;
        assert repository.collectDependency(project, Scope.Runtime).size() == 1;
        assert repository.collectDependency(project, Scope.Test).size() == 0;
        assert repository.collectDependency(project, Scope.System).size() == 0;
    }

    @Test
    void runtime_system() {
        TemporaryProject project = new TemporaryProject();
        project.require("one", one -> {
            one.require("nest").atSystem();
        }).atRuntime();

        assert repository.collectDependency(project, Scope.Annotation).size() == 0;
        assert repository.collectDependency(project, Scope.Compile).size() == 0;
        assert repository.collectDependency(project, Scope.Provided).size() == 0;
        assert repository.collectDependency(project, Scope.Runtime).size() == 1;
        assert repository.collectDependency(project, Scope.Test).size() == 0;
        assert repository.collectDependency(project, Scope.System).size() == 0;
    }

    @Test
    void runtime_runtime() {
        TemporaryProject project = new TemporaryProject();
        project.require("one", one -> {
            one.require("nest").atRuntime();
        }).atRuntime();

        assert repository.collectDependency(project, Scope.Annotation).size() == 0;
        assert repository.collectDependency(project, Scope.Compile).size() == 0;
        assert repository.collectDependency(project, Scope.Provided).size() == 0;
        assert repository.collectDependency(project, Scope.Runtime).size() == 2;
        assert repository.collectDependency(project, Scope.Test).size() == 0;
        assert repository.collectDependency(project, Scope.System).size() == 0;
    }

    @Test
    void annotation_compile() {
        TemporaryProject project = new TemporaryProject();
        project.require("one", one -> {
            one.require("nest");
        }).atAnnotation();

        assert repository.collectDependency(project, Scope.Annotation).size() == 2;
        assert repository.collectDependency(project, Scope.Compile).size() == 0;
        assert repository.collectDependency(project, Scope.Provided).size() == 0;
        assert repository.collectDependency(project, Scope.Runtime).size() == 0;
        assert repository.collectDependency(project, Scope.Test).size() == 0;
        assert repository.collectDependency(project, Scope.System).size() == 0;
    }

    @Test
    void annotation_test() {
        TemporaryProject project = new TemporaryProject();
        project.require("one", one -> {
            one.require("nest").atTest();
        }).atAnnotation();

        assert repository.collectDependency(project, Scope.Annotation).size() == 1;
        assert repository.collectDependency(project, Scope.Compile).size() == 0;
        assert repository.collectDependency(project, Scope.Provided).size() == 0;
        assert repository.collectDependency(project, Scope.Runtime).size() == 0;
        assert repository.collectDependency(project, Scope.Test).size() == 0;
        assert repository.collectDependency(project, Scope.System).size() == 0;
    }

    @Test
    void annotation_annotation() {
        TemporaryProject project = new TemporaryProject();
        project.require("one", one -> {
            one.require("nest").atAnnotation();
        }).atAnnotation();

        assert repository.collectDependency(project, Scope.Annotation).size() == 1;
        assert repository.collectDependency(project, Scope.Compile).size() == 0;
        assert repository.collectDependency(project, Scope.Provided).size() == 0;
        assert repository.collectDependency(project, Scope.Runtime).size() == 0;
        assert repository.collectDependency(project, Scope.Test).size() == 0;
        assert repository.collectDependency(project, Scope.System).size() == 0;
    }

    @Test
    void annotation_provided() {
        TemporaryProject project = new TemporaryProject();
        project.require("one", one -> {
            one.require("nest").atProvided();
        }).atAnnotation();

        assert repository.collectDependency(project, Scope.Annotation).size() == 1;
        assert repository.collectDependency(project, Scope.Compile).size() == 0;
        assert repository.collectDependency(project, Scope.Provided).size() == 0;
        assert repository.collectDependency(project, Scope.Runtime).size() == 0;
        assert repository.collectDependency(project, Scope.Test).size() == 0;
        assert repository.collectDependency(project, Scope.System).size() == 0;
    }

    @Test
    void annotation_system() {
        TemporaryProject project = new TemporaryProject();
        project.require("one", one -> {
            one.require("nest").atSystem();
        }).atAnnotation();

        assert repository.collectDependency(project, Scope.Annotation).size() == 1;
        assert repository.collectDependency(project, Scope.Compile).size() == 0;
        assert repository.collectDependency(project, Scope.Provided).size() == 0;
        assert repository.collectDependency(project, Scope.Runtime).size() == 0;
        assert repository.collectDependency(project, Scope.Test).size() == 0;
        assert repository.collectDependency(project, Scope.System).size() == 0;
    }

    @Test
    void annotation_runtime() {
        TemporaryProject project = new TemporaryProject();
        project.require("one", one -> {
            one.require("nest").atRuntime();
        }).atAnnotation();

        assert repository.collectDependency(project, Scope.Annotation).size() == 2;
        assert repository.collectDependency(project, Scope.Compile).size() == 0;
        assert repository.collectDependency(project, Scope.Provided).size() == 0;
        assert repository.collectDependency(project, Scope.Runtime).size() == 0;
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
            this(RandomStringUtils.randomAlphabetic(5) + "-root");
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
            TemporaryProject project = new TemporaryProject(getProduct().substring(0, getProduct().indexOf('-')) + "-" + productName);

            definition.accept(project);
            repository.install(project);

            return require(project.getGroup(), project.getProduct(), project.getVersion());
        }
    }

}