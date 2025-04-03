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

import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

import bee.api.Command;
import bee.task.sample.ByNameExternal;

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
    void defaultCommnad() {
        interface A extends Task {
            @Command("")
            default void command() {
            }
        }

        TaskInfo info = TaskInfo.by(A.class);
        assert info.defaultCommnad.equals("command");
    }

    @Test
    void defaultCommnadWithSpecified() {
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
    void defaultCommnadWithoutSpecified() {
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
    void commands() {
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
    void commandNeedAnnotaion() {
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
    @SuppressWarnings("unused")
    void commandFromParentTask() {
        interface A extends Task {
            @Command("")
            default void command1() {
            }

            default void command2() {
            }
        }

        interface B extends A {

            @Command("")
            default void command3() {
            }
        }

        TaskInfo info = TaskInfo.by(B.class);
        assert info.commands.containsKey("command1");
        assert info.commands.containsKey("command2") == false;
        assert info.commands.containsKey("command3");
    }

    @Test
    @SuppressWarnings("unused")
    void commandOverride() {
        interface A extends Task {
            @Command("")
            default void command1() {
            }

            default void command2() {
            }
        }

        interface B extends A {
            @Override
            @Command("")
            default void command1() {
            }

            @Override
            @Command("")
            default void command2() {
            }
        }

        TaskInfo info = TaskInfo.by(B.class);
        assert info.commands.containsKey("command1");
        assert info.commands.containsKey("command2");
        assert info.commands.get("command1").getDeclaringClass() == B.class;
        assert info.commands.get("command2").getDeclaringClass() == B.class;
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
    void descriptions() {
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
        assert info.task == bee.ByName.class;

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
    void taskIsProxy() {
        interface A extends Task {
            @Command("")
            default void some() {
            }
        }

        A task = TaskInfo.find(A.class);
        assert Proxy.isProxyClass(task.getClass());
    }

    @Test
    void proxyToString() {
        interface A extends Task {
            @Command("")
            default void some() {
            }
        }

        A task = TaskInfo.find(A.class);
        assert task.toString().equals("Task [a]");
    }

    @Test
    void proxyHashCode() {
        interface A extends Task {
            @Command("")
            default void some() {
            }
        }

        A task = TaskInfo.find(A.class);
        assert task.hashCode() == System.identityHashCode(task);
    }

    @Test
    void proxyEquals() {
        interface A extends Task {
            @Command("")
            default void some() {
            }
        }

        A task = TaskInfo.find(A.class);
        assert task.equals(task);
        assert task.equals(TaskInfo.find(A.class));
        assert task.equals(null) == false;
        assert task.equals(new Object()) == false;
    }

    @Test
    void emptyTask() {
        interface Empty extends Task {
        }

        Empty task = TaskInfo.find(Empty.class);
        assert Proxy.isProxyClass(task.getClass());
    }
}
