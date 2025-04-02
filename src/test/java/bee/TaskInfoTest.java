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
import kiss.I;

class TaskInfoTest extends AutomaticProjectAware {
    static {
        I.load(TaskInfoTest.class);
    }

    @Test
    void byClass() {
        interface A extends Task {
            @Command("")
            default String command() {
                return "byClass";
            }
        }

        TaskInfo info = TaskInfo.by(A.class);
        assert info != null;
        assert info.name.equals("a");
        assert info.task == A.class;

        A task = (A) info.create();
        assert Proxy.isProxyClass(task.getClass());
        assert task.command().equals("byClass");
    }

    @Test
    void byName() {
        class Project extends AutoProject {
            interface A extends Task {
                @Command("")
                default String command() {
                    return "byName";
                }
            }
        };

        TaskInfo info = TaskInfo.by("a");
        assert info != null;
        assert info.name.equals("a");
        assert info.task == Project.A.class;

        Project.A task = (Project.A) info.create();
        assert Proxy.isProxyClass(task.getClass());
        assert task.command().equals("byName");
    }
}
