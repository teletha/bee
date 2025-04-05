/*
 * Copyright (C) 2025 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

import bee.api.Command;
import kiss.I;
import psychopath.Directory;
import psychopath.File;
import psychopath.Locator;

class TaskInfoTest extends InlineProjectAware {

    @Test
    void name() {
        interface A extends Task {
            @Command("")
            default void command() {
            }
        }

        TaskInfo info = TaskInfo.by(A.class);
        assert info.name.equals("a");
    }

    @Test
    void task() {
        interface A extends Task {
            @Command("")
            default void command() {
            }
        }

        TaskInfo info = TaskInfo.by(A.class);
        assert info.task == A.class;
    }

    @Test
    void defaultCommandSingle() {
        interface A extends Task {
            @Command("")
            default void command() {
            }
        }

        TaskInfo info = TaskInfo.by(A.class);
        assert info.defaultCommand.equals("command");
    }

    @Test
    void defaultCommandSpecified() {
        interface A extends Task {
            @Command("")
            default void command1() {
            }

            @Command(value = "", defaults = true)
            default void command2() {
            }
        }

        TaskInfo info = TaskInfo.by(A.class);
        assert info.defaultCommand.equals("command2");
    }

    @Test
    void defaultCommandUnspecifiedMultiple() {
        interface A extends Task {
            @Command("")
            default void command1() {
            }

            @Command("")
            default void command2() {
            }
        }

        TaskInfo info = TaskInfo.by(A.class);
        assert info.defaultCommand.equals("help");
    }

    @Test
    void defaultCommandSameAsTaskName() {
        interface MyTask extends Task {
            @Command("")
            default void otherCommand() {
            }

            @Command("")
            default void myTask() {
            }
        }
        TaskInfo info = TaskInfo.by(MyTask.class);
        assert info.name.equals("my-task");
        assert info.defaultCommand.equals("my-task");
    }

    @Test
    void commandsDiscovery() {
        interface A extends Task {
            @Command("")
            default void command1() {
            }

            @Command("")
            default void command2() {
            }
        }

        TaskInfo info = TaskInfo.by(A.class);
        assert info.commands.containsKey("command1");
        assert info.commands.containsKey("command2");
        assert info.commands.containsKey("help");
    }

    @Test
    @SuppressWarnings("unused")
    void commandRequiresAnnotation() {
        interface A extends Task {
            @Command("")
            default void command1() {
            }

            default void command2() {
            }
        }

        TaskInfo info = TaskInfo.by(A.class);
        assert info.commands.containsKey("command1");
        assert info.commands.containsKey("command2") == false;
        assert info.commands.containsKey("help");
    }

    @Test
    void commandFromParent() {
        interface ParentTask extends Task {
            @Command("")
            default void commandParent() {
            }
        }
        interface ChildTask extends ParentTask {
            @Command("")
            default void commandChild() {
            }
        }

        TaskInfo info = TaskInfo.by(ChildTask.class);
        assert info.commands.containsKey("command-parent");
        assert info.commands.containsKey("command-child");
    }

    @Test
    @SuppressWarnings("unused")
    void commandOverrideWithAnnotation() {
        interface ParentOverrideTask extends Task {
            @Command("Parent")
            default void commandOverridden() {
            }

            default void commandNotOverridden() {
            }
        }
        interface ChildOverrideTask extends ParentOverrideTask {
            @Override
            @Command("Child")
            default void commandOverridden() {
            }

            @Override
            @Command("Child Annotated")
            default void commandNotOverridden() {
            } // Adding annotation in child
        }

        TaskInfo info = TaskInfo.by(ChildOverrideTask.class);
        assert info.commands.containsKey("command-overridden");
        assert info.commands.containsKey("command-not-overridden");
        assert ChildOverrideTask.class == info.commands.get("command-overridden").getDeclaringClass();
        assert ChildOverrideTask.class == info.commands.get("command-not-overridden").getDeclaringClass();
        assert info.descriptions.get("command-overridden").equals("Child");
        assert info.descriptions.get("command-not-overridden").equals("Child Annotated");
    }

    @Test
    void commandOverrideWithoutAnnotation() {
        interface A extends Task {
            @Command("")
            default void command() {
            }
        }

        interface B extends A {
            @Override
            default void command() {
            }
        }

        TaskInfo info = TaskInfo.by(B.class);
        assert info.commands.containsKey("command");
        assert info.commands.get("command").getDeclaringClass() == A.class;
    }

    @Test
    void descriptionsMapping() {
        interface A extends Task {
            @Command("desc")
            default void command() {
            }
        }

        TaskInfo info = TaskInfo.by(A.class);
        assert info.descriptions.get("command").equals("desc");
    }

    @Test
    void descriptionsOverride() {
        interface A extends Task {
            @Command("parent")
            default void command() {
            }
        }

        interface B extends A {
            @Command("child")
            @Override
            default void command() {
            }
        }

        TaskInfo info = TaskInfo.by(B.class);
        assert info.descriptions.get("command").equals("child");
    }

    @Test
    void configClassResolution() {
        class MyConfig {
        }

        interface ConfiguredTask extends Task<MyConfig> {
            @Command("")
            default void exec() {
            }
        }

        TaskInfo info = TaskInfo.by(ConfiguredTask.class);
        assert info.config == MyConfig.class;
    }

    @Test
    void configClassResolutionObject() {
        interface NonConfiguredTask extends Task {
            @Command("")
            default void exec() {
            }
        }

        TaskInfo info = TaskInfo.by(NonConfiguredTask.class);
        assert info.config == Object.class;
    }

    @Test
    void configClassResolutionWithInheritance() {
        class ParentConfig {
        }
        interface ParentTask<C> extends Task<C> {
            @Command("")
            default void parentExec() {
            }
        }
        interface ChildTask extends ParentTask<ParentConfig> {
            @Command("")
            default void childExec() {
            }
        }

        TaskInfo info = TaskInfo.by(ChildTask.class);
        assert info.config == ParentConfig.class;
    }

    @Test
    void byClass() {
        interface ByClass extends Task {
            @Command("")
            default String command() {
                return "byClass";
            }
        }

        TaskInfo info = TaskInfo.by(ByClass.class);
        assert info != null;
        assert info.name.equals("by-class");
        assert info.task == ByClass.class;

        ByClass task = (ByClass) info.create();
        assert Proxy.isProxyClass(task.getClass());
        assert task.command().equals("byClass");
    }

    @Test
    void byClassNotTask() {
        class NotATask {
        }

        assertThrows(Fail.class, () -> TaskInfo.by(NotATask.class));
    }

    @SuppressWarnings("unused")
    @Test
    void byClassWithProxy() {
        interface ProxiedTask extends Task {
            @Command("")
            default void exec() {
            }
        }

        // Create a dummy proxy
        ProxiedTask proxy = I.make(ProxiedTask.class, (p, m, a) -> null);

        TaskInfo info = TaskInfo.by(proxy.getClass());
        assert info.name.equals("proxied-task");
        assert info.task == ProxiedTask.class;
    }

    /**
     * Tests the task resolution priority when a task with the same name exists
     * both as an inner class of the current project and in the same package as the project.
     * Expects the inner class task to be prioritized.
     */
    @Test
    void byNameInProjectAndSamePackage() {
        class Project extends InlineProject {
            interface SameName extends Task {
                @Command("")
                default String command() {
                    return "sameName in project";
                }
            }
        };

        // Verify that a task with the same name exists directly under this package (bee.SameName).
        Class<?> innerTaskClass = Project.SameName.class;
        Class<?> packageTaskClass = bee.SameName.class;
        assert innerTaskClass != packageTaskClass;
        assert innerTaskClass.getPackage() == packageTaskClass.getPackage();

        // Resolve the task by name "same-name".
        TaskInfo info = TaskInfo.by("same-name");

        // Assert that the resolved task is the inner class one.
        assert info.name.equals("same-name");
        assert info.task == innerTaskClass; // Should prioritize the inner class task
        assert info.task != packageTaskClass;

        // Verify the created task instance executes the inner class command.
        Project.SameName task = (Project.SameName) info.create();
        assert task.command().equals("sameName in project");
    }

    /**
     * Tests the task resolution priority when a task with the same name exists
     * both in the same package as the current project context (test class) and in a different
     * package. Expects the task in the same package to be prioritized.
     */
    @Test
    void byNameInSamePackageAndOtherPackage() {
        // Identify the two tasks with the same name "same-name".
        Class<?> samePackageTaskClass = bee.SameName.class;
        Class<?> otherPackageTaskClass = bee.task.sample.SameName.class;
        assert samePackageTaskClass != otherPackageTaskClass;
        assert samePackageTaskClass.getPackage() != otherPackageTaskClass.getPackage();

        // Resolve the task by name "same-name".
        TaskInfo info = TaskInfo.by("same-name");

        // Assert that the resolved task is the one from the same package.
        assert info.name.equals("same-name");
        assert info.task == samePackageTaskClass; // Should prioritize the task in the same package
        assert info.task != otherPackageTaskClass;

        // Verify the created task instance executes the same package command.
        bee.SameName task = (bee.SameName) info.create();
        assert task.command().equals("sameName in same package");
    }

    /**
     * Tests the task resolution priority when a task with the same name exists
     * in one package and another instance (loaded from a different source/classloader but with the
     * same FQCN) also exists. This simulates scenarios like having the same library loaded twice.
     * Expects the task loaded from the "original" classpath location to be prioritized over
     * the one loaded from the temporary source, based on `Locator.locate()` comparison.
     */
    @Test
    void byNameInOtherPackageAndOtherSource() {
        // Identify the original task class from a specific package.
        Class<?> originalTaskClass = bee.task.sample.SameNameExternalPackage.class;

        // Define and load another version of the same class from a temporary location.
        Class<?> loadedTaskClass = defineAndLoadFromOtherSource(originalTaskClass);

        // Sanity checks to ensure the loading worked as expected.
        assert originalTaskClass != loadedTaskClass;
        assert originalTaskClass.getPackage() != loadedTaskClass.getPackage();
        assert Locator.locate(originalTaskClass).equals(Locator.locate(loadedTaskClass)) == false;

        // Resolve the task by name "same-name-external-package".
        TaskInfo info = TaskInfo.by("same-name-external-package");

        // Assert that the resolved task is the original one (from the main classpath).
        assert info.name.equals("same-name-external-package");
        assert info.task == originalTaskClass; // Should prioritize the original location
        assert info.task != loadedTaskClass;

        // Verify the created task instance executes the original command.
        bee.task.sample.SameNameExternalPackage task = (bee.task.sample.SameNameExternalPackage) info.create();
        assert task.command().equals("same name in external package");
    }

    /**
     * Helper method to simulate loading a class from a different source (JAR/directory).
     * Copies the bytecode of the given class to a temporary directory and loads it using a
     * custom classloader. It also registers the loaded task class with TaskInfo.
     *
     * @param originalClass The original class to load from another source.
     * @return The {@link Class} object loaded from the temporary source.
     */
    private Class defineAndLoadFromOtherSource(Class originalClass) {
        // Locate the original class file.
        File source = Locator.file("target/test-classes/" + originalClass.getName().replace('.', '/') + ".class");

        // Create a temporary directory structure matching the package.
        Directory temp = Locator.temporaryDirectory();
        source.copyTo(temp.directory(originalClass.getPackageName().replace('.', '/')));

        // Use a custom classloader to load the class from the temporary directory.
        // PriorityClassLoader might be specific to Bee's testing setup.
        try (PriorityClassLoader loader = new PriorityClassLoader(Null.UI)) {
            loader.addClassPath(temp);
            loader.highPriorityClasses.add(originalClass.getName());

            Class<Task> loaded = (Class<Task>) loader.loadClass(originalClass.getName());
            // Assert basic properties to confirm loading from a different source.
            assert originalClass != loaded : "Loaded class should be a different object.";
            assert originalClass.getName().equals(loaded.getName()) : "Class names should match.";
            // Package objects will differ due to different classloaders.
            assert originalClass.getPackage() != loaded.getPackage() : "Package objects should differ.";
            assert originalClass.getPackage().getName().equals(loaded.getPackage().getName()) : "Package names should match.";
            // Source locations (JAR/directory) should differ.
            assert !Locator.locate(originalClass).equals(Locator.locate(loaded)) : "Source locations should differ.";

            return loaded;
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    @Test
    void byNameNotFound() {
        assertThrows(Fail.class, () -> TaskInfo.by("non-existent-task##"));
    }

    @Test
    void byNameNotFoundWithTypo() {
        interface Typo extends Task {
            @Command("")
            default void command() {
            }
        }

        TaskInfo info = TaskInfo.by("type");
        assert info.name.equals("typo");
        assert info.task == Typo.class;
    }

    @Test
    void byNameNullOrBlank() {
        assertThrows(Fail.class, () -> TaskInfo.by((String) null), "Specify task name.");
        assertThrows(Fail.class, () -> TaskInfo.by(""), "Specify task name.");
        assertThrows(Fail.class, () -> TaskInfo.by("  "), "Specify task name.");
    }

    @Test
    void computeTaskNameFromMethodReference() {
        interface MyRefTask extends Task {
            @Command("")
            default void test() {
            }
        }

        String computedName = TaskInfo.computeTaskName(MyRefTask::test);
        assert computedName.equals("my-ref-task:test");
    }

    @Test
    void computeTaskNameFromValuedMethodReference() {
        interface MyValRefTask extends Task {
            @Command("")
            default String value() {
                return "value";
            }
        }

        String computedName = TaskInfo.computeTaskName(MyValRefTask::value);
        assert computedName.equals("my-val-ref-task:value");
    }

    @Test
    void taskIsProxy() {
        interface A extends Task {
            @Command("")
            default void some() {
            }
        }

        A task = Task.by(A.class);
        assert Proxy.isProxyClass(task.getClass());
    }

    @Test
    void proxyToString() {
        interface A extends Task {
            @Command("")
            default void some() {
            }
        }

        A task = Task.by(A.class);
        assert task.toString().equals("Task [a]");
    }

    @Test
    void proxyHashCode() {
        interface A extends Task {
            @Command("")
            default void some() {
            }
        }

        A task = Task.by(A.class);
        assert task.hashCode() == System.identityHashCode(task);
    }

    @Test
    void proxyEquals() {
        interface A extends Task {
            @Command("")
            default void some() {
            }
        }

        A task = Task.by(A.class);
        assert task.equals(task);
        assert task.equals(Task.by(A.class));
        assert task.equals(null) == false;
        assert task.equals(new Object()) == false;
    }

    @Test
    void emptyTask() {
        interface Empty extends Task {
        }

        Empty task = Task.by(Empty.class);
        assert Proxy.isProxyClass(task.getClass());
    }

    @Test
    void rejectConcreteTask() {
        class Invalid implements Task {
        }

        assertThrows(Fail.class, () -> TaskInfo.by(Invalid.class));
    }

    @Test
    void rejectAbstractTask() {
        abstract class Invalid implements Task {
        }

        assertThrows(Fail.class, () -> TaskInfo.by(Invalid.class));
    }
}
