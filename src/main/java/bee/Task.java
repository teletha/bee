/*
 * Copyright (C) 2024 The BEE Development Team
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
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;

import bee.api.Command;
import kiss.Extensible;
import kiss.Managed;
import kiss.Model;

@Managed(value = TaskLifestyle.class)
public abstract class Task implements Extensible {

    /**
     * Execute manual tasks.
     */
    public void execute() {
        // do nothing
    }

    @Command("Display help message for all commands of this task.")
    public void help() {
        TaskInfo info = TaskInfo.by(TaskInfo.computeTaskName(getClass()));

        for (Entry<String, String> entry : info.descriptions.entrySet()) {
            // display usage description for this command
            ui().info(entry.getKey(), " - ", entry.getValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Model.of(this).type.getSimpleName();
    }

    /**
     * Type-safe task referenece.
     */
    public interface TaskReference<T> extends Consumer<T>, Serializable {
    }

    /**
     * Type-safe task reference which can return a value.
     */
    public interface ValuedTaskReference<T, R> extends Function<T, R>, Serializable, TaskReference<T> {

        @Override
        default void accept(T task) {
            apply(task);
        }
    }
}