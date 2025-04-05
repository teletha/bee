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

import static bee.TaskOperations.*;

import java.io.Serializable;
import java.util.function.Consumer;
import java.util.function.Function;

import bee.api.Command;
import bee.api.Project;
import kiss.Extensible;

/**
 * Defines the basic structure for all tasks within the Bee build system.
 * <p>
 * Tasks are defined as interfaces extending this {@code Task} interface.
 * They declare their available operations as methods annotated with {@link Command}.
 * </p>
 * <p>
 * Tasks can be parameterized with a configuration class ({@code C}) to manage
 * task-specific settings. These settings are typically stored per project.
 * </p>
 * <p>
 * Use the static factory method {@link #by(Class)} to obtain an executable instance
 * of a task interface.
 * </p>
 *
 * @param <C> The type of the configuration class specific to this task. If no specific
 *            configuration is needed, this can be omitted or {@link Object} can be used.
 */
public interface Task<C> extends Extensible {

    /**
     * Displays a detailed help message for this task, including descriptions of all
     * available commands and configuration options. This command is available by default
     * for all tasks.
     * <p>
     * Implementation relies on {@link TaskInfo#describe(UserInterface)} to gather and format
     * the task's metadata.
     * </p>
     */
    @Command("Display help message for all commands of this task.")
    default void help() {
        // Retrieves the TaskInfo associated with the actual implementing task class (proxy),
        // then calls describe on the current UserInterface.
        TaskInfo.by(getClass()).describe(ui());
    }

    /**
     * Retrieves the configuration object associated with the current project for this specific
     * task.
     * <p>
     * The configuration class is determined by reflecting on the generic type parameter {@code C}
     * of this {@code Task} interface implementation. The actual configuration instance is managed
     * per project using {@link Project#associate(Class)}.
     * </p>
     * <p>
     * If the task interface is not parameterized (i.e., {@code C} is effectively {@code Object}),
     * this method returns {@code null}.
     * </p>
     *
     * @return The project-specific configuration instance of type {@code C}, or {@code null}
     *         if the task is not parameterized with a configuration class.
     */
    default C config() {
        return (C) TaskOperations.config(getClass());
    }

    /**
     * Static factory method to obtain an executable instance of a specified task interface.
     * <p>
     * This method uses {@link TaskInfo} to find the metadata for the given task interface
     * and creates a dynamic proxy instance that handles command execution, caching, and
     * UI interactions.
     * </p>
     *
     * @param <T> The specific type of the task interface.
     * @param task The {@link Class} object representing the task interface (e.g.,
     *            {@code Compile.class}).
     *            Must be an interface extending {@link Task}.
     * @return An executable proxy instance implementing the requested task interface {@code T}.
     * @throws Fail if the provided class is not a valid task interface or if its metadata cannot be
     *             found.
     */
    static <T extends Task> T by(Class<T> task) {
        return (T) TaskInfo.by(task).create();
    }

    /**
     * Represents a type-safe reference to a task command, typically implemented using a
     * method reference (e.g., {@code MyTask::myCommand}).
     * <p>
     * This functional interface serves as a marker and must be {@link Serializable}
     * to allow Bee to potentially introspect the reference (e.g., using
     * {@link TaskInfo#computeTaskName(TaskReference)}).
     * </p>
     * It extends {@link Consumer} to indicate that it accepts a task instance to operate on.
     *
     * @param <T> The type of the {@link Task} interface containing the referenced command.
     */
    public interface TaskReference<T> extends Consumer<T>, Serializable {
        // No methods defined here; inherits accept(T) from Consumer.
    }

    /**
     * Represents a type-safe reference to a task command that returns a value,
     * typically implemented using a method reference (e.g., {@code MyTask::getBuildResult}).
     * <p>
     * This functional interface serves as a marker and must be {@link Serializable}.
     * </p>
     * It extends {@link Function} to indicate that it accepts a task instance and produces a
     * result. It also extends {@link TaskReference} for consistency.
     *
     * @param <T> The type of the {@link Task} interface containing the referenced command.
     * @param <R> The type of the value returned by the referenced command.
     */
    public interface ValuedTaskReference<T, R> extends Function<T, R>, Serializable, TaskReference<T> {
        /**
         * {@inheritDoc}
         * <p>
         * Provides a default implementation for the {@link Consumer#accept(Object)} method
         * inherited via {@link TaskReference}. It simply calls the {@link Function#apply(Object)}
         * method, effectively executing the valued task command but discarding its return value.
         * </p>
         * 
         * @param task The task instance on which to execute the command.
         */
        @Override
        default void accept(T task) {
            // Execute the function but ignore the result when used as a Consumer.
            apply(task);
        }
    }
}