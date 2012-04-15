/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map.Entry;

import kiss.I;
import kiss.model.ClassUtil;
import bee.UserInterface;
import bee.api.Project;
import bee.util.Inputs;

/**
 * @version 2012/04/15 14:26:46
 */
public abstract class Task {

    /** The current processing project. */
    protected final Project project = I.make(Project.class);

    /** The user interface. */
    protected final UserInterface ui = I.make(UserInterface.class);

    @Command(description = "Display help message for all commands of this task.")
    public void help() {

        root: for (Entry<Method, List<Annotation>> entry : ClassUtil.getAnnotations(getClass()).entrySet()) {
            String name = entry.getKey().getName();

            if (!name.equals("help")) {
                for (Annotation annotation : entry.getValue()) {
                    if (annotation.annotationType() == Command.class) {
                        Command command = (Command) annotation;

                        // display usage description for this commnad
                        ui.talk(name);
                        ui.talk("    ");

                        continue root;
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Use other task from task specific API.
     * </p>
     * 
     * @param taskClass A task class.
     * @return A target task.
     */
    protected <T extends Task> T require(Class<T> taskClass) {
        return I.make(TaskManager.class).find(taskClass);
    }

    /**
     * <p>
     * Compute human-readable task name.
     * </p>
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
}
