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

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import bee.Task.TaskReference;
import bee.api.Command;
import bee.api.Project;
import bee.util.Inputs;
import kiss.I;
import kiss.Model;

/**
 * 
 */
public class TaskInfo {

    /** The task name. */
    private final String name;

    /** The task definition. */
    final Class<Task> task;

    /** The default command name. */
    String defaultCommnad = "help";

    /** The actual commands. */
    final Map<String, Method> commands = new TreeMap();

    /** The command descriptions. */
    final Map<String, String> descriptions = new TreeMap();

    /** The common task repository. */
    static Map<String, TaskInfo> commons;

    /**
     * @param name
     * @param task
     */
    TaskInfo(String name, Class<Task> task) {
        this.name = name;
        this.task = task;

        for (Entry<Method, List<Annotation>> info : Model.collectAnnotatedMethods(task).entrySet()) {
            for (Annotation annotation : info.getValue()) {
                if (annotation.annotationType() == Command.class) {
                    Method method = info.getKey();

                    // compute command name
                    Command command = (Command) annotation;
                    String commnadName = Inputs.hyphenize(method.getName());

                    // register
                    commands.put(commnadName, method);

                    if (!commnadName.equals("help")) {
                        descriptions.put(commnadName, command.value());
                    }

                    if (command.defaults()) {
                        defaultCommnad = commnadName;
                    }
                }
            }
        }

        // search default command
        if (descriptions.size() == 1) {
            defaultCommnad = descriptions.keySet().iterator().next();
        } else if (descriptions.containsKey(name)) {
            defaultCommnad = name;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("%-8s \t%s", name, descriptions.get(defaultCommnad));
    }

    /**
     * Find the task instance by type.
     * 
     * @param <T>
     * @param type
     * @return
     */
    public static final <T extends Task> T find(Class<T> type) {
        return (T) by(computeTaskName(type)).create();
    }

    /**
     * Compute human-readable task name.
     * 
     * @param taskClass A target task.
     * @return A task name.
     */
    public static final String computeTaskName(Class taskClass) {
        if (taskClass.isSynthetic()) {
            return computeTaskName(taskClass.getSuperclass());
        }
        return Inputs.hyphenize(taskClass.getSimpleName());
    }

    /**
     * Compute human-readable task name.
     * 
     * @param task A target task.
     * @return A task name.
     */
    static final <T extends Task> String computeTaskName(TaskReference<T> task) {
        try {
            Method m = task.getClass().getDeclaredMethod("writeReplace");
            m.setAccessible(true);

            SerializedLambda s = (SerializedLambda) m.invoke(task);
            Method method = I.type(s.getImplClass().replaceAll("/", ".")).getMethod(s.getImplMethodName());

            return computeTaskName(method.getDeclaringClass()) + ":" + method.getName().toLowerCase();
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    /**
     * Find task information by name.
     * 
     * @param name A task name.
     * @return A specified task.
     */
    static final TaskInfo by(String name) {
        if (name == null) {
            throw new Error("You must specify task name.");
        }

        synchronized (Task.class) {
            if (commons == null || !commons.containsKey(name)) {
                commons = new TreeMap();
                for (Class<Task> task : I.findAs(Task.class)) {
                    String taskName = TaskInfo.computeTaskName(task);
                    TaskInfo info = new TaskInfo(taskName, task);
                    if (!info.descriptions.isEmpty()) {
                        commons.put(taskName, info);
                    }
                }
            }
        }

        // search from common tasks
        TaskInfo info = commons.get(name);

        if (info == null) {
            // Search for tasks with similar names for possible misspellings.
            String recommend = Inputs.recommend(name, commons.keySet());
            if (recommend != null && I.make(UserInterface.class).confirm("Isn't it a misspelling of task [" + recommend + "] ?")) {
                return commons.get(recommend);
            }

            Fail failure = new Fail("Task [" + name + "] is not found. You can use the following tasks.");
            for (TaskInfo i : commons.values()) {
                failure.solve(i);
            }
            throw failure;
        }

        // API definition
        return info;
    }

    /**
     * @return
     */
    public Task create() {
        return I.make(task, (object, method, args) -> {
            String commnadName = Inputs.hyphenize(method.getName());

            if (commands.containsKey(commnadName)) {
                String taskName = Inputs.hyphenize(task.getSimpleName()) + ":" + commnadName;
                Project project = TaskOperations.project();
                Cache cache = project.associate(Cache.class);
                Object result = cache.get(taskName);
                if (!cache.containsKey(taskName)) {
                    UserInterface ui = TaskOperations.ui();
                    try {
                        ui.startCommand(taskName, null);
                        result = MethodHandles.lookup().unreflectSpecial(method, task).bindTo(object).invokeWithArguments();
                        cache.put(taskName, result);
                    } finally {
                        ui.endCommand(taskName, null);
                    }
                }
                return result;
            } else {
                return MethodHandles.lookup().unreflectSpecial(method, task).bindTo(object).invokeWithArguments();
            }
        });
    }

    /**
     * Store for each command result.
     */
    @SuppressWarnings("serial")
    private static class Cache extends HashMap<String, Object> {
    }
}