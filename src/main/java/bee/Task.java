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
import java.lang.reflect.Type;
import java.util.function.Consumer;
import java.util.function.Function;

import bee.api.Command;
import kiss.Extensible;
import kiss.Model;

public interface Task<C> extends Extensible {

    @Command("Display help message for all commands of this task.")
    default void help() {
        UserInterface ui = ui();
        TaskInfo info = TaskInfo.by(getClass());

        ui.info("Task [", info.name, "] has the following commands :");
        ui.info(info.descriptions);
    }

    /**
     * Get the user configuration which is associated with the project.
     * 
     * @return
     */
    default C config() {
        Type[] types = Model.collectParameters(getClass(), Task.class);
        if (types.length == 0) {
            return null;
        } else {
            return project().associate((Class<C>) types[0]);
        }
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