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

public class TaskTest extends AbstractTaskTest {

    static {
        I.load(TaskTest.class);
    }

    @Test
    void taskIsProxy() {
        interface Some extends Task {
            @Command("")
            default void some() {
            }
        }

        Some task = TaskInfo.find(Some.class);
        assert Proxy.isProxyClass(task.getClass());
    }

    @Test
    void proxyToString() {
        interface Some extends Task {
            @Command("")
            default void some() {
            }
        }

        Some task = TaskInfo.find(Some.class);
        assert task.toString().equals("some");
        assert Proxy.isProxyClass(task.getClass());
    }

    @Test
    void invalidEmptyTask() {
        interface Empty extends Task {
        }

        Empty task = TaskInfo.find(Empty.class);
        assert Proxy.isProxyClass(task.getClass());
    }
}
