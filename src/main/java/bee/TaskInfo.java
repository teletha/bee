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
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bee.Task.TaskReference;
import bee.api.Command;
import bee.api.Comment;
import bee.api.Project;
import bee.util.Inputs;
import kiss.I;
import kiss.Model;
import psychopath.Location;
import psychopath.Locator;

/**
 * Represents metadata about a task, including its computed name, defining class,
 * available commands, command descriptions, and configuration details.
 * This class provides static methods to find and manage task information,
 * and a method to describe the task's details to a user interface.
 * It acts as a central registry and factory for tasks within Bee.
 */
class TaskInfo {

    /** The current processing task name holder. */
    static final InheritableThreadLocal<String> current = new InheritableThreadLocal();

    /** Cache mapping task interface classes to their {@link TaskInfo}. */
    static final Map<Class, TaskInfo> types = new ConcurrentHashMap();

    /**
     * Registry mapping task names (hyphenated) to a list of {@link TaskInfo} objects.
     * This supports multiple task implementations potentially sharing the same base name,
     * allowing for context-based resolution (e.g., prioritizing tasks defined in the current
     * project).
     */
    static final Map<String, List<TaskInfo>> names = new ConcurrentSkipListMap();

    /** The computed, hyphenated task name (e.g., "compile"). Derived from the task class name. */
    final String name;

    /**
     * The interface class that defines the task logic and commands (e.g., {@code Compile.class}).
     */
    final Class<? extends Task> task;

    /**
     * The class defining the configuration specific to this task.
     * Derived from the generic type parameter of the {@link Task} interface (e.g.,
     * {@code Task<MyConfig>}).
     * Defaults to {@link Object Object.class} if no specific configuration type is specified.
     */
    final Class config;

    /**
     * The name of the command executed by default when only the task name is invoked.
     * Determined by {@link Command#defaults()}, single command presence, or matching the task name.
     * Defaults to "help" if no other default is identified.
     */
    String defaultCommand = "help";

    /**
     * A map of command names (hyphenated) to the corresponding {@link Method} objects
     * representing the executable command within the task interface or its super-interfaces.
     * Includes the implicit "help" command.
     */
    final Map<String, Method> commands = new ConcurrentSkipListMap();

    /**
     * A map of command names (hyphenated) to their user-friendly descriptions,
     * sourced from the {@link Command#value()} annotation attribute.
     * Excludes the implicit "help" command's description.
     */
    final Map<String, String> descriptions = new ConcurrentSkipListMap();

    /**
     * Constructs a {@link TaskInfo} instance by introspecting the given task interface.
     * It computes the task name, discovers methods annotated with {@link Command},
     * populates the command and description maps, determines the default command,
     * identifies the configuration class via generics, and prepares for describing
     * configuration fields later.
     *
     * @param task The interface representing the task definition (must implement {@link Task} and
     *            be an interface).
     * @throws RuntimeException if introspection fails due to unexpected reflection errors.
     *             Specific validation failures (e.g., not an interface) are usually caught
     *             by the {@code by} methods.
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
                            defaultCommand = commnadName;
                        }
                    }
                }
            }

            // search default command
            if (descriptions.size() == 1) {
                defaultCommand = descriptions.keySet().iterator().next();
            } else if (descriptions.containsKey(name)) {
                defaultCommand = name;
            }

            // Determine configuration class from Task<C> generic parameter
            Type[] params = Model.collectParameters(task, Task.class);
            this.config = params.length == 0 ? Object.class : (Class) params[0];
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    /**
     * Describes the details of this task to the provided user interface.
     * Displays the task name, default command, defining class, available commands with
     * descriptions,
     * and configurable fields (from the {@link #config} class) with their descriptions
     * (from {@link Comment}) and current values within the current project context.
     *
     * @param ui The {@link UserInterface} to display the information on. Must not be null.
     */
    void describe(UserInterface ui) {
        ui.title(String.format("  %-12s\tdefault [%s]\tclass [%s]", name.toUpperCase(), defaultCommand, task.getName()));

        // Commands Section
        if (!descriptions.isEmpty()) {
            ui.info("Command");
            ui.info(descriptions);
        } else {
            ui.info("Command");
            ui.info("  No explicit commands defined, only default 'help'");
        }

        // Configuration Section
        if (config != Object.class) {
            ui.info("Configuration");
            ui.info(Stream.of(config.getFields()).filter(field -> field.isAnnotationPresent(Comment.class)).map(field -> {
                try {
                    String fieldName = field.getName();
                    String comment = field.getAnnotation(Comment.class).value();
                    String typeName = type(field.getGenericType());
                    Object currentValue = field.get(TaskOperations.config(task));

                    // Avoid printing synthetic class names (e.g., for lambdas used as default
                    // values)
                    if (currentValue != null && currentValue.getClass().isSynthetic()) {
                        currentValue = null;
                    }

                    if (currentValue == null) {
                        return String.format("%-12s\t%s (%s)", fieldName, comment, typeName);
                    } else {
                        return String.format("%-12s\t%s (%s : %s)", fieldName, comment, typeName, currentValue);
                    }
                } catch (Exception x) {
                    throw I.quiet(x);
                }
            }).toList());
        }
    }

    /**
     * Converts a {@link Type} into a human-readable string representation.
     * Handles common type structures like classes, parameterized types, arrays, type variables,
     * wildcards, and generic arrays. Provides clear and concise type names suitable for display.
     *
     * @param type The {@link Type} to format.
     * @return A string representation of the type (e.g., "String", "List<Integer>", "Map<String, ?
     *         extends Number>[]"). Returns an empty string if the input type is null.
     */
    private String type(Type type) {
        if (type == null) {
            return "";
        } else if (type instanceof Class clazz) {
            return clazz.getSimpleName();
        } else if (type instanceof ParameterizedType param) {
            return type(param.getRawType()) + Stream.of(param.getActualTypeArguments())
                    .map(this::type)
                    .collect(Collectors.joining(", ", "<", ">"));
        } else if (type instanceof TypeVariable variable) {
            return variable.getName();
        } else if (type instanceof WildcardType wild) {
            Type[] upper = wild.getUpperBounds();
            Type[] lower = wild.getLowerBounds();
            if (lower.length > 0) {
                return "?" + Stream.of(lower).map(this::type).collect(Collectors.joining(" & ", " super ", ""));
            } else if (upper.length == 0 || upper[0] == Object.class) {
                return "?";
            } else {
                return "?" + Stream.of(upper).map(this::type).collect(Collectors.joining(" & ", " extends ", ""));
            }
        } else if (type instanceof GenericArrayType array) {
            return type(array.getGenericComponentType()) + "[]";
        } else {
            // Fallback for unknown types, using the detailed but potentially complex built-in name
            return type.getTypeName();
        }
    }

    /**
     * Creates a dynamic proxy instance of the task interface represented by this {@link TaskInfo}.
     * The returned proxy implements the task interface ({@link #task}) and intercepts
     * command method calls using an Interceptor. This enables features like command execution
     * caching and user interface notifications during task execution.
     *
     * @return A new proxy instance implementing the task interface, ready for command execution.
     */
    Task create() {
        return I.make(task, new Interceptor(task, commands.keySet()));
    }

    /**
     * Returns a simple string representation of this task, suitable for listings.
     * It includes the task name (left-aligned) and the description of its default command.
     * Example: {@code "compile      Compile Java source files"}
     *
     * @return A formatted string summarizing the task.
     */
    @Override
    public String toString() {
        return String.format("%-12s \t%s", name, descriptions.get(defaultCommand));
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
     * Retrieves the {@link TaskInfo} associated with the given task interface.
     * It performs validation (must be an interface implementing {@link Task}), handles potential
     * proxy classes, uses an internal cache, and triggers task registration if the interface info
     * is not cached.
     *
     * @param task The task interface (must implement {@link Task}).
     * @return The corresponding {@link TaskInfo}.
     * @throws NullPointerException if task is null.
     * @throws Fail if the class is not a valid Task interface, an unusable proxy, or if info cannot
     *             be created.
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
                    .orElseThrow(() -> new Fail(task + " must implement single specific task interface.")));
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
                Fail failure = new Fail("Task [" + name + "] is not found. Available tasks are following :");
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
            if (!types.containsKey(type) && type != Task.class) {
                TaskInfo info = new TaskInfo(type);
                if (!info.commands.isEmpty()) {
                    types.put(type, info);
                    names.computeIfAbsent(info.name, key -> new ArrayList()).add(info);
                }
            }
        }
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

            if (commands.contains(commnadName)) {
                // ===============================================================
                // Handle Task commands
                // ===============================================================
                String taskName = Inputs.hyphenize(task.getSimpleName()) + ":" + commnadName;
                Project project = TaskOperations.project();
                UserInterface ui = TaskOperations.ui();

                return project.associate(Cache.class).computeIfAbsent(taskName, key -> I.Jobs.submit(() -> {
                    ForProject.local.set(project);
                    ForUI.local.set(ui);
                    current.set(taskName);

                    try {
                        ui.startCommand(key, null);
                        Object result = MethodHandles.lookup().unreflectSpecial(method, task).bindTo(proxy).invokeWithArguments(args);
                        return result;
                    } catch (Throwable e) {
                        throw new Fail("Task [" + taskName + "] was failed.").reason(e);
                    } finally {
                        ui.endCommand(key, null);
                    }
                })).get();
            } else {
                // ===============================================================
                // Handle Object methods
                // ===============================================================
                return switch (method.getName()) {
                case "toString" -> "Task [" + computeTaskName(task) + "]";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> {
                    Object other = args[0];
                    if (other == null || !Proxy.isProxyClass(other.getClass())) yield false;
                    yield Proxy.getInvocationHandler(other) instanceof Interceptor interceptor && interceptor.task == task;
                }

                // ===============================================================
                // Handle other interface methods (e.g., default methods)
                // ===============================================================
                // Use MethodHandles to invoke potentially default methods on the proxy itself
                default -> MethodHandles.lookup().unreflectSpecial(method, task).bindTo(proxy).invokeWithArguments(args);
                };
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
    private static class Cache extends ConcurrentHashMap<String, Future> {
        // No additional logic needed, inherits HashMap behavior.
        // Association with Project handles the scoping.
    }
}