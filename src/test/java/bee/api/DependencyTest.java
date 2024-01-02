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

import java.util.Random;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import antibug.CleanRoom;
import bee.BlinkProject;
import psychopath.Locator;

@Execution(ExecutionMode.SAME_THREAD)
class DependencyTest {

    @RegisterExtension
    private static CleanRoom room = new CleanRoom();

    private TemporaryProject project;

    private Repository repository;

    @BeforeEach
    void setup() {
        project = new TemporaryProject();
        repository = new Repository(project);
        repository.setLocalRepository(Locator.directory(room.locateRadom()));
    }

    @Test
    void empty() {
        assert repository.collectDependency(project, Scope.Compile).size() == 0;
    }

    @Test
    void atCompile() {
        project.require("org.ow2.asm", "asm", "9.2");

        assert repository.collectDependency(project, Scope.Annotation).size() == 0;
        assert repository.collectDependency(project, Scope.Compile).size() == 1;
        assert repository.collectDependency(project, Scope.Provided).size() == 0;
        assert repository.collectDependency(project, Scope.Runtime).size() == 1;
        assert repository.collectDependency(project, Scope.Test).size() == 0;
        assert repository.collectDependency(project, Scope.System).size() == 0;
    }

    @Test
    void atTest1() {
        project.require("org.ow2.asm", "asm", "9.2").atTest();

        assert repository.collectDependency(project, Scope.Annotation).size() == 0;
        assert repository.collectDependency(project, Scope.Compile).size() == 0;
        assert repository.collectDependency(project, Scope.Provided).size() == 0;
        assert repository.collectDependency(project, Scope.Runtime).size() == 0;
        assert repository.collectDependency(project, Scope.Test).size() == 1;
        assert repository.collectDependency(project, Scope.System).size() == 0;
    }

    @Test
    void atTest2() {
        project.require("org.ow2.asm", "asm-tree", "5.0.4").atTest();

        assert repository.collectDependency(project, Scope.Test).size() == 2;
        assert repository.collectDependency(project, Scope.Runtime).size() == 0;
    }

    @Test
    void atAnnotation() {
        project.require("org.atteo.classindex", "classindex", "3.4").atAnnotation();

        Repository repository = new Repository(project);
        assert repository.collectDependency(project, Scope.Annotation).size() == 1;
        assert repository.collectDependency(project, Scope.Runtime).size() == 0;
    }

    @Test
    void atProvided1() {
        BlinkProject project = new BlinkProject();
        project.require("org.ow2.asm", "asm", "9.2").atProvided();

        assert repository.collectDependency(project, Scope.Compile).size() == 1;
        assert repository.collectDependency(project, Scope.Provided).size() == 1;
        assert repository.collectDependency(project, Scope.Runtime).size() == 0;
    }

    @Test
    void atProvided2() {
        project.require("org.ow2.asm", "asm", "9.2");
        project.require("org.ow2.asm", "asm-util", "9.2").atProvided();

        assert repository.collectDependency(project, Scope.Compile).size() == 4;
        assert repository.collectDependency(project, Scope.Provided).size() == 4;
        assert repository.collectDependency(project, Scope.Runtime).size() == 1;
    }

    @Test
    void external1() {
        project.require("org.skyscreamer", "jsonassert", "1.2.3");

        assert repository.collectDependency(project, Scope.Runtime).size() == 2;
        assert repository.collectDependency(project, Scope.Test, Scope.Compile).size() == 2;
    }

    @Test
    void byLibrary() {
        Library library = new Library("org.skyscreamer", "jsonassert", "1.2.3");
        assert repository.collectDependency(library, Scope.Runtime).size() == 2;
        assert repository.collectDependency(library, Scope.Test, Scope.Compile).size() == 2;
    }

    @Test
    void byLibraryWithClassifier() {
        Library library = new Library("org.bytedeco", "javacv-platform", "1.3.1");
        assert repository.collectDependency(library, Scope.Test, Scope.Compile).size() == 84;
    }

    @Test
    void compile_compile() {
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
    private class TemporaryProject extends BlinkProject {

        /**
         * 
         */
        private TemporaryProject() {
            this(generateRandomString(5) + "-root");
        }

        private static String generateRandomString(int length) {
            String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
            StringBuilder randomString = new StringBuilder();

            Random random = new Random();
            for (int i = 0; i < length; i++) {
                int randomIndex = random.nextInt(characters.length());
                randomString.append(characters.charAt(randomIndex));
            }
            return randomString.toString();
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