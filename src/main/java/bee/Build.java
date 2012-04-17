/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee;

import kiss.I;
import bee.api.Project;
import bee.task.Task;
import bee.task.TaskManager;

/**
 * <p>
 * Project build process.
 * </p>
 * 
 * @version 2012/04/17 9:16:37
 */
public abstract class Build {

    /**
     * <p>
     * Create task.
     * </p>
     * 
     * @param taskClass
     * @return
     */
    protected final <T extends Task> T task(Class<T> taskClass) {
        return I.make(TaskManager.class).find(taskClass);
    }

    /**
     * <p>
     * Build project.
     * </p>
     * 
     * @param project
     */
    protected abstract void build(Project project);
}
