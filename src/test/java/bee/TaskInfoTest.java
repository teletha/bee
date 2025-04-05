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
import bee.task.sample.ByNameExternal;
import kiss.I;

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
        assert info.defaultCommnad.equals("command");
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
        assert info.defaultCommnad.equals("command2");
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
        assert info.defaultCommnad.equals("help");
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
        assert info.defaultCommnad.equals("my-task");
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

    @Test
    void byNameInProject() {
        class Project extends InlineProject {
            interface ByName extends Task {
                @Command("")
                default String command() {
                    return "byName in project";
                }
            }
        };

        TaskInfo info = TaskInfo.by("by-name");
        assert info != null;
        assert info.name.equals("by-name");
        assert info.task == Project.ByName.class;

        Project.ByName task = (Project.ByName) info.create();
        assert Proxy.isProxyClass(task.getClass());
        assert task.command().equals("byName in project");
    }

    @Test
    @SuppressWarnings("unused")
    void byNameInSamePackage() {
        class Project extends InlineProject {
        };

        TaskInfo info = TaskInfo.by("by-name");
        assert info != null;
        assert info.name.equals("by-name");
        assert info.task == ByName.class;

        ByName task = (ByName) info.create();
        assert Proxy.isProxyClass(task.getClass());
        assert task.command().equals("byName in same package");
    }

    @Test
    @SuppressWarnings("unused")
    void byNameInOtherPackage() {
        class Project extends InlineProject {
        };

        TaskInfo info = TaskInfo.by("by-name-external");
        assert info != null;
        assert info.name.equals("by-name-external");
        assert info.task == ByNameExternal.class;

        ByNameExternal task = (ByNameExternal) info.create();
        assert Proxy.isProxyClass(task.getClass());
        assert task.command().equals("byName in external package");
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
    void denyConcreteTask() {
        class Invalid implements Task {
        }

        assertThrows(Fail.class, () -> Task.by(Invalid.class));
    }

    @Test
    void denyAbstractTask() {
        abstract class Invalid implements Task {
        }

        assertThrows(Fail.class, () -> Task.by(Invalid.class));
    }
}
