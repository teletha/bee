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
 * Represents metadata about a task, including its computed name, defining class,
 * available commands, and command descriptions. This class provides static methods
 * to find and manage task information.
 */
public class TaskInfo {

    /** Cache mapping task implementation classes to their TaskInfo. */
    private static final Map<Class, TaskInfo> types = new ConcurrentHashMap();

    /**
     * Registry mapping task names to a list of TaskInfo objects (supports multiple tasks with the
     * same name).
     */
    private static final Map<String, List<TaskInfo>> names = new ConcurrentSkipListMap();

    /** The computed, hyphenated task name (e.g., "compile"). */
    final String name;

    /** The class that defines the task logic and commands. */
    final Class<? extends Task> task;

    /**
     * The name of the command that is executed by default if no specific command is provided.
     * Defaults to "help".
     */
    String defaultCommnad = "help";

    /**
     * A map of command names (hyphenated) to the corresponding {@link Method} objects within the
     * task class.
     */
    final Map<String, Method> commands = new ConcurrentSkipListMap();

    /**
     * A map of command names (hyphenated) to their descriptions, sourced from the {@link Command}
     * annotation.
     */
    final Map<String, String> descriptions = new ConcurrentSkipListMap();

    /**
     * Constructs a {@link TaskInfo} instance by introspecting the given task class.
     * It computes the task name, discovers methods annotated with {@link Command},
     * populates the command and description maps, and determines the default command.
     *
     * @param task The class representing the task definition (must implement {@link Task}).
     * @throws RuntimeException if introspection fails (e.g., reflection errors).
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

                        // register command method and description
                        commands.put(commnadName, method);

                        // exclude built-in help description implicitly
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
     * Returns a simple string representation of this task, typically used for listing available
     * tasks. It includes the task name and the description of its default command.
     *
     * @return A formatted string (e.g., "compile \tCompile source files").
     */
    @Override
    public String toString() {
        return String.format("%-8s \t%s", name, descriptions.get(defaultCommnad));
    }

    /**
     * Finds and returns an executable instance of the specified task type.
     * This method retrieves the {@link TaskInfo} for the class and creates a proxy task.
     *
     * @param <T> The type of the task.
     * @param support The class type of the task to find.
     * @return An executable (potentially proxy) instance of the specified task type.
     */
    public static final <T extends Task> T find(Class<T> support) {
        return (T) by(support).create();
    }

    /**
     * Computes a standardized, hyphenated task name from a task class name.
     * For example, {@code CompileTask.class} would likely become {@code "compile-task"}.
     *
     * @param taskClass The task class.
     * @return The computed hyphenated task name.
     */
    private static final String computeTaskName(Class taskClass) {
        return Inputs.hyphenize(taskClass.getSimpleName());
    }

    /**
     * Computes a task name from a {@link TaskReference} (functional interface, typically a lambda
     * or method reference). This uses {@link SerializedLambda} introspection to find the
     * implementing method and its declaring class. The format is usually
     * "declaring-task-name:method-name".
     *
     * @param <T> The type of the task.
     * @param task The task reference (e.g., {@code MyTasks::compile}).
     * @return The computed task name (e.g., "my-tasks:compile").
     * @throws RuntimeException if introspection using SerializedLambda fails.
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
     * Retrieves the {@link TaskInfo} associated with the given task class.
     * It performs validation, handles potential proxy classes, uses an internal cache, and triggers
     * task registration if the class info is not cached.
     *
     * @param task The task class (must implement {@link Task}).
     * @return The corresponding {@link TaskInfo}.
     * @throws NullPointerException if task is null.
     * @throws Fail if the class is not a valid Task implementation or an unusable proxy.
     */
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

        if (!task.isInterface()) {
            throw new Fail(task + " must be interface.");
        }

        // search task by type
        TaskInfo info = types.get(task);
        if (info == null) {
            register();
            info = types.get(task);

            if (info == null) {
                // Should ideally not happen if registration works and class is valid, but could if
                // the task has no @Command methods. For robustness, could throw here or return a
                // dummy TaskInfo. The original code implicitly relies on registration populating
                // the map.
                throw new Fail("Task information for " + task.getName() + " could not be found or created (possibly no @Command methods).");
            }
        }

        // API definition
        return info;
    }

    /**
     * Retrieves the {@link TaskInfo} associated with the given task name.
     * It uses an internal cache, triggers task registration if the name is not
     * cached, handles potential ambiguity if multiple tasks share the same name (prioritizing by
     * project context), and suggests alternatives for mistyped names.
     *
     * @param name The hyphenated task name (e.g., "compile").
     * @return The corresponding {@link TaskInfo}.
     * @throws Fail if the name is blank, the task cannot be found (potentially with suggestions),
     *             or if ambiguity resolution fails (though current logic tries hard to resolve).
     */
    static TaskInfo by(String name) {
        if (name == null || name.isBlank()) {
            throw new Fail("Specify task name.");
        }

        // check cache, trigger registration if name not found
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

        // handle multiple tasks with the same name
        if (infomation.size() != 1) {
            // 1. Prioritize task defined within the current project class (inner class)
            Project currentProject = TaskOperations.project();
            Class currentProjectClass = currentProject.getClass();
            for (TaskInfo info : infomation) {
                if (info.task.getEnclosingClass() == currentProjectClass) {
                    return info;
                }
            }

            // 2. Prioritize task in the same package as the current project
            Package currentProjectPackage = currentProjectClass.getPackage();
            for (TaskInfo info : infomation) {
                // Ensure it's not an inner class (already checked) and packages match
                if (!info.task.isMemberClass() && info.task.getPackage().equals(currentProjectPackage)) {
                    return info;
                }
            }

            // 3. Prioritize task originating from the same location (e.g., same JAR) as the current
            // project
            Location currentProjectLocation = Locator.locate(currentProjectClass);
            for (TaskInfo info : infomation) {
                // Ensure it's not an inner class and locations match
                if (!info.task.isMemberClass() && Locator.locate(info.task).equals(currentProjectLocation)) {
                    return info;
                }
            }
            // If no context match, fall through to return the last one found (arbitrary but
            // deterministic)
        }

        // Return the single found task or the last one after ambiguity checks
        return infomation.getLast();
    }

    /**
     * Scans the classpath for classes implementing {@link Task}. For each new, valid task found
     * (i.e., has {@link Command} methods), it creates a {@link TaskInfo} instance and populates
     * the internal caches ({@link #types} and {@link #names}). This method ensures that available
     * tasks are discoverable.
     */
    private static synchronized void register() {
        for (Class<Task> type : I.findAs(Task.class)) {
            if (!types.containsKey(type)) {
                TaskInfo info = new TaskInfo(type);
                if (!info.commands.isEmpty()) {
                    types.put(type, info);
                    names.computeIfAbsent(info.name, key -> new ArrayList()).add(info);
                }
            }
        }
    }

    /**
     * Creates a proxy instance of the task represented by this {@link TaskInfo}.
     * The proxy intercepts command method calls to add behavior like caching and UI notifications
     * via the interceptor.
     *
     * @return A new proxy instance implementing the task interface.
     */
    Task create() {
        return I.make(task, new Interceptor(task, commands.keySet()));
    }

    /**
     * The {@link InvocationHandler} used for task proxies created by {@link TaskInfo#create()}.
     * It intercepts method calls:
     * <ul>
     * <li>If the method corresponds to a registered command, it checks a project-specific cache,
     * executes the command if not cached (with UI notifications), stores the result, and returns
     * it.</li>
     * <li>Handles standard {@link Object} methods ({@code toString}, {@code hashCode},
     * {@code equals}) appropriately for proxies.</li>
     * <li>Delegates other non-command method calls directly to the underlying implementation (if
     * possible/needed, though tasks usually only expose commands).</li>
     * </ul>
     */
    @SuppressWarnings("serial")
    private static class Interceptor implements InvocationHandler, Serializable {

        /** The target task class (used for method reflection). */
        private final Class task;

        /**
         * A set of known command names for quick lookup during invocation. Transient because
         * Methods aren't serializable.
         */
        private final transient Set<String> commands;

        /**
         * Constructs an Interceptor.
         * 
         * @param task The target task class.
         * @param commands The set of hyphenated command names defined in the task.
         */
        private Interceptor(Class task, Set<String> commands) {
            this.task = task;
            this.commands = commands;
        }

        /**
         * Handles method invocation on the proxy instance.
         *
         * @param proxy The proxy instance that the method was invoked on.
         * @param method The {@code Method} instance corresponding to the interface method invoked
         *            on the proxy instance.
         * @param args An array of objects containing the values of the arguments passed in the
         *            method invocation on the proxy instance.
         * @return The value to return from the method invocation on the proxy instance.
         * @throws Throwable The exception to throw from the method invocation on the proxy
         *             instance.
         */
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String commnadName = Inputs.hyphenize(method.getName());

            // Check if the invoked method is a registered command
            if (commands.contains(commnadName)) {
                String taskName = Inputs.hyphenize(task.getSimpleName()) + ":" + commnadName;
                Project project = TaskOperations.project();
                Cache cache = project.associate(Cache.class);
                Object result = cache.get(taskName);

                // Check cache first. Using containsKey allows caching null results.
                // [[ get(taskName) == null ]] is ambiguous.
                if (!cache.containsKey(taskName)) {
                    UserInterface ui = TaskOperations.ui();
                    try {
                        ui.startCommand(taskName, null);
                        result = MethodHandles.lookup().unreflectSpecial(method, task).bindTo(proxy).invokeWithArguments(args);
                        cache.put(taskName, result);
                    } finally {
                        ui.endCommand(taskName, null);
                    }
                }
                return result;
            } else {
                // Handle standard Object methods
                if (method.getDeclaringClass() == Object.class) {
                    String name = method.getName();
                    if (name.equals("toString")) {
                        return "Task [" + computeTaskName(task) + "]";
                    } else if (name.equals("hashCode")) {
                        return System.identityHashCode(proxy);
                    } else if (name.equals("equals")) {
                        Object other = args[0];
                        if (other == proxy) return true;
                        if (other == null || !Proxy.isProxyClass(other.getClass())) return false;
                        InvocationHandler otherHandler = Proxy.getInvocationHandler(other);
                        if (otherHandler instanceof Interceptor) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                }

                // Handle other interface methods (e.g., default methods)
                // Use MethodHandles to invoke potentially default methods on the proxy itself
                return MethodHandles.lookup().unreflectSpecial(method, task).bindTo(proxy).invokeWithArguments(args);
            }
        }
    }

    /**
     * A simple cache associated with a {@link Project} instance via
     * {@link Project#associate(Class)}.
     * It stores the results of task command executions within that project's context, using the
     * fully qualified command name (e.g., "compile:java") as the key. This prevents re-execution
     * of the same command within a single build lifecycle for that project.
     */
    @SuppressWarnings("serial")
    private static class Cache extends HashMap<String, Object> {
        // No additional logic needed, inherits HashMap behavior.
        // Association with Project handles the scoping.
    }
}