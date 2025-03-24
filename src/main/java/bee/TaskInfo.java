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

import static bee.TaskOperations.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import bee.Task.ParallelInterface;
import bee.api.Command;
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
    public static <T extends Task> T find(Class<T> type) {
        return (T) I.make(by(computeTaskName(type)).task);
    }

    /**
     * Compute human-readable task name.
     * 
     * @param taskClass A target task.
     * @return A task name.
     */
    static final String computeTaskName(Class taskClass) {
        if (taskClass.isSynthetic()) {
            return computeTaskName(taskClass.getSuperclass());
        }
        return Inputs.hyphenize(taskClass.getSimpleName());
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
}