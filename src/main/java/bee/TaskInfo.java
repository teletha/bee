/*
 * Copyright (C) 2025 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import bee.Task.TaskReference;
import bee.api.Command;
import bee.api.Project;
import bee.util.Inputs;
import kiss.I;
import kiss.Model;
import psychopath.Location;
import psychopath.Locator;

/**
 * Represents information about a task, including its name, commands, and descriptions.
 */
public class TaskInfo {

    private static final Map<Class, TaskInfo> types = new ConcurrentHashMap();

    /** The common task repository. */
    private static final Map<String, List<TaskInfo>> names = new ConcurrentSkipListMap();

    /** The task name. */
    final String name;

    /** The task definition. */
    final Class<? extends Task> task;

    /** The default command name. */
    String defaultCommnad = "help";

    /** The actual commands. */
    final Map<String, Method> commands = new ConcurrentSkipListMap();

    /** The command descriptions. */
    final Map<String, String> descriptions = new ConcurrentSkipListMap();

    /**
     * Constructs a TaskInfo instance for the specified task.
     * 
     * @param task The class representing the task.
     */
    TaskInfo(Class<? extends Task> task) {
        try {
            this.name = computeTaskName(task);
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
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    /**
     * Returns a string representation of this task.
     * 
     * @return A formatted string containing the task name and its default command description.
     */
    @Override
    public String toString() {
        return String.format("%-8s \t%s", name, descriptions.get(defaultCommnad));
    }

    /**
     * Finds the task instance by its type.
     * 
     * @param <T> The type of task.
     * @param support The class type of the task.
     * @return An instance of the specified task type.
     */
    public static final <T extends Task> T find(Class<T> support) {
        return (T) by(support).create();
    }

    /**
     * Computes a human-readable task name from a class.
     * 
     * @param taskClass The task class.
     * @return The computed task name.
     */
    private static final String computeTaskName(Class taskClass) {
        return Inputs.hyphenize(taskClass.getSimpleName());
    }

    /**
     * Computes a human-readable task name from a TaskReference.
     * 
     * @param task The task reference.
     * @return The computed task name.
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

    static TaskInfo by(Class task) {
        // validate task type
        Objects.requireNonNull(task);

        if (!Task.class.isAssignableFrom(task)) {
            throw new Fail(task + " must implement task interface.");
        }

        if (Proxy.isProxyClass(task)) {
            return by(Model.collectTypes(task)
                    .stream()
                    .filter(type -> type != task && type != Task.class && Task.class.isAssignableFrom(type))
                    .findFirst()
                    .orElseThrow(() -> new Fail(task + " must implement single task.")));
        }

        // search task by type
        TaskInfo info = types.get(task);
        if (info == null) {
            register();
            info = types.get(task);
        }

        // API definition
        return info;
    }

    /**
     * Finds task information by name.
     * 
     * @param name The task name.
     * @return The corresponding TaskInfo instance.
     */
    static TaskInfo by(String name) {
        if (name == null || name.isBlank()) {
            throw new Fail("Specify task name.");
        }

        if (!names.containsKey(name)) {
            register();
        }

        // search task by name
        List<TaskInfo> infomation = names.get(name);

        if (infomation == null) {
            // Search for tasks with similar names for possible misspellings.
            String recommend = Inputs.recommend(name, names.keySet());
            if (recommend == null) {
                Fail failure = new Fail("Task [" + name + "] is not found. You can use the following tasks.");
                for (List<TaskInfo> list : names.values()) {
                    failure.solve(list.get(0));
                }
                throw failure;
            }
            infomation = names.get(recommend);
        }

        if (infomation.size() != 1) {
            // Since there are multiple tasks registered with this name, select them according to
            // their priority. First, the task defined in the Project currently being processed is
            // selected as the highest priority.
            Project currentProject = TaskOperations.project();
            Class currentProjectClass = currentProject.getClass();
            for (TaskInfo info : infomation) {
                if (info.task.getEnclosingClass() == currentProjectClass) {
                    return info;
                }
            }

            // Next, select a task that belongs to the same package as the current processing
            // project.
            Package currentProjectPackage = currentProjectClass.getPackage();
            for (TaskInfo info : infomation) {
                if (info.task.getPackage().equals(currentProjectPackage)) {
                    return info;
                }
            }

            // Finally, select a task that has the same origin as the current processing project.
            Location currentProjectLocation = Locator.locate(currentProjectClass);
            for (TaskInfo info : infomation) {
                if (Locator.locate(info.task).equals(currentProjectLocation)) {
                    return info;
                }
            }
        }

        // API definition
        return infomation.getLast();
    }

    private static void register() {
        for (Class<Task> type : I.findAs(Task.class)) {
            TaskInfo info = new TaskInfo(type);
            if (!info.commands.isEmpty()) {
                types.put(type, info);
                names.computeIfAbsent(info.name, key -> new ArrayList()).add(info);
            }
        }
    }

    /**
     * Creates a proxy instance of the task.
     * 
     * @return The created task instance.
     */
    Task create() {
        return I.make(task, new Interceptor(task, commands.keySet()));
    }

    @SuppressWarnings("serial")
    private static class Interceptor implements InvocationHandler, Serializable {

        private final Class task;

        private final transient Set<String> commands;

        private Interceptor(Class task, Set<String> commands) {
            this.task = task;
            this.commands = commands;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object invoke(Object object, Method method, Object[] args) throws Throwable {
            String commnadName = Inputs.hyphenize(method.getName());

            if (commands.contains(commnadName)) {
                String taskName = Inputs.hyphenize(task.getSimpleName()) + ":" + commnadName;
                Project project = TaskOperations.project();
                Cache cache = project.associate(Cache.class);
                Object result = cache.get(taskName);
                if (!cache.containsKey(taskName)) {
                    UserInterface ui = TaskOperations.ui();
                    try {
                        ui.startCommand(taskName, null);
                        result = MethodHandles.lookup().unreflectSpecial(method, task).bindTo(object).invokeWithArguments(args);
                        cache.put(taskName, result);
                    } finally {
                        ui.endCommand(taskName, null);
                    }
                }
                return result;
            } else {
                if (method.getDeclaringClass() == Object.class) {
                    String name = method.getName();
                    if (name.equals("toString")) {
                        return "Task [" + computeTaskName(task) + "]";
                    } else if (name.equals("hashCode")) {
                        return name.hashCode();
                    } else if (name.equals("equals")) {
                        Object other = args[0];
                        if (other == object) return true;
                        if (other == null || !Proxy.isProxyClass(other.getClass())) return false;
                        InvocationHandler otherHandler = Proxy.getInvocationHandler(other);
                        if (otherHandler instanceof Interceptor) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                }
                return MethodHandles.lookup().unreflectSpecial(method, task).bindTo(object).invokeWithArguments(args);
            }
        }
    }

    /**
     * Store for each command result.
     */
    @SuppressWarnings("serial")
    private static class Cache extends HashMap<String, Object> {
    }
}