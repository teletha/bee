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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map.Entry;
import java.util.Set;

import kiss.I;
import kiss.model.ClassUtil;
import bee.Bee;
import bee.UserInterface;
import bee.api.Library;
import bee.api.Project;
import bee.api.Repository;
import bee.api.Scope;
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
    protected final <T extends Task> T require(Class<T> taskClass) {
        return I.make(TaskManager.class).find(taskClass);
    }

    /**
     * <p>
     * Use other library from task specific API.
     * </p>
     * 
     * @param group
     * @param product
     * @param version
     * @return
     */
    protected final Set<Library> load(String group, String product, String version) {
        return I.make(Repository.class).collectDependency(group, product, version, Scope.Runtime);
    }

    /**
     * <p>
     * Use Bee library.
     * </p>
     * 
     * @return
     */
    protected final Path loadBee() {
        return ClassUtil.getArchive(Bee.class);
    }

    /**
     * <p>
     * Utility method for task.
     * </p>
     * 
     * @param path
     */
    protected final void makeDirectory(Path path) {
        if (path != null && Files.notExists(path)) {
            try {
                Files.createDirectories(path);

                ui.talk("Make directory [" + path.toAbsolutePath() + "]");
            } catch (IOException e) {
                throw I.quiet(e);
            }
        }
    }

    /**
     * <p>
     * Utility method for task.
     * </p>
     * 
     * @param path
     */
    protected final void makeDirectory(Path base, String path) {
        makeDirectory(base.resolve(path));
    }
}
