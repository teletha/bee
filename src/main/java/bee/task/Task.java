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

import java.util.Map.Entry;

import kiss.I;
import bee.UserInterface;
import bee.api.Project;
import bee.task.TaskManager.TaskInfo;

/**
 * @version 2012/04/15 14:26:46
 */
public abstract class Task {

    /** The current processing project. */
    protected final Project project = I.make(Project.class);

    /** The user interface. */
    protected final UserInterface ui = I.make(UserInterface.class);

    @Command("Display help message for all commands of this task.")
    public void help() {
        TaskInfo info = I.make(TaskManager.class).find(TaskManager.computeTaskName(getClass()));

        for (Entry<String, String> entry : info.descriptions.entrySet()) {
            // display usage description for this command
            ui.talk(entry.getKey(), " - ", entry.getValue());
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

}
