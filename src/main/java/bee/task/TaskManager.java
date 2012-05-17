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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import kiss.ClassListener;
import kiss.I;
import kiss.Manageable;
import kiss.Singleton;
import kiss.model.ClassUtil;
import bee.UserInterface;
import bee.api.Project;
import bee.util.Inputs;

/**
 * @version 2012/04/15 14:13:34
 */
@Manageable(lifestyle = Singleton.class)
public class TaskManager implements ClassListener<Task> {

    /** The common task repository. */
    private final Map<String, TaskInfo> commons = new HashMap();

    /** The project specific task repository. */
    private final Map<Path, Map<String, TaskInfo>> projects = new HashMap();

    /**
     * <p>
     * Execute task.
     * </p>
     * 
     * @param input User task input.
     */
    public void execute(String input) {
        // parse command
        if (input == null) {
            return;
        }

        // remove head and tail white space
        input = input.trim();

        if (input.length() == 0) {
            return;
        }

        // analyze task name
        String taskName = "";
        String commandName = "";
        int index = input.indexOf(':');

        if (index == -1) {
            taskName = input;
        } else {
            taskName = input.substring(0, index);
            commandName = input.substring(index + 1);
        }

        // search task
        TaskInfo info = find(taskName);

        if (commandName.isEmpty()) {
            commandName = info.defaultCommnad;
        }

        // search command
        Method command = info.commands.get(commandName.toLowerCase());

        if (command == null) {
            throw new Error("Task [" + taskName + "] doesn't has the command [" + commandName + "].");
        }

        // create task and initialize
        Task task = I.make(info.task);

        // execute task

        try {
            command.invoke(task);
        } catch (Throwable e) {
            if (e instanceof InvocationTargetException) {
                e = ((InvocationTargetException) e).getTargetException();
            }
            I.make(UserInterface.class).error(e);
        }
    }

    /**
     * <p>
     * Find task by class.
     * </p>
     * 
     * @param taskClass
     */
    public <T extends Task> T find(Class<T> taskClass) {
        if (taskClass == null) {
            throw new Error("You must specify task class.");
        }
        return (T) I.make(find(computeTaskName(taskClass)).task);
    }

    /**
     * <p>
     * Find task by name.
     * </p>
     * 
     * @param name A task name.
     * @return A specified task.
     */
    TaskInfo find(String name) {
        if (name == null) {
            throw new Error("You must specify task name.");
        }

        Project project = I.make(Project.class);
        TaskInfo info = null;

        // search from project specified tasks
        for (Entry<Path, Map<String, TaskInfo>> entry : projects.entrySet()) {
            Path path = entry.getKey();

            if (path.startsWith(project.getRoot())) {
                info = entry.getValue().get(name);

                if (info != null) {
                    break;
                }
            }
        }

        if (info == null) {
            // search from common tasks
            info = commons.get(name);

            if (info == null) {
                throw new Error("Task [" + name + "] is not found.");
            }
        }

        // API definition
        return info;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load(Class<Task> clazz) {
        String name = computeTaskName(clazz);
        Path archive = ClassUtil.getArchive(clazz);

        if (Files.isRegularFile(archive)) {
            // common task
            commons.put(name, new TaskInfo(name, clazz));
        } else {
            // project specified task
            Map<String, TaskInfo> infos = projects.get(archive);

            if (infos == null) {
                infos = new HashMap();
                projects.put(archive, infos);
            }
            infos.put(name, new TaskInfo(name, clazz));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unload(Class<Task> clazz) {
        String name = computeTaskName(clazz);
        Path archive = ClassUtil.getArchive(clazz);

        if (Files.isRegularFile(archive)) {
            // common task
            commons.remove(name);
        } else {
            // project specified task
            Map<String, TaskInfo> infos = projects.get(archive);

            if (infos != null) {
                infos.remove(name);
            }
        }
    }

    /**
     * <p>
     * Compute human-readable task name.
     * </p>
     * 
     * @param taskClass A target task.
     * @return A task name.
     */
    static final String computeTaskName(Class taskClass) {
        if (taskClass.isSynthetic()) {
            return computeTaskName(taskClass.getSuperclass());
        }
        return Inputs.hyphenize(taskClass.getSimpleName());
    }

    /**
     * @version 2012/05/17 14:55:28
     */
    static final class TaskInfo {

        /** The task definition. */
        private final Class<Task> task;

        /** The default command name. */
        String defaultCommnad = "help";

        /** The actual commands. */
        final Map<String, Method> commands = new HashMap();

        /** The command descriptions. */
        final Map<String, String> descriptions = new HashMap();

        /**
         * @param name
         * @param task
         */
        private TaskInfo(String name, Class<Task> task) {
            this.task = task;

            for (Entry<Method, List<Annotation>> info : ClassUtil.getAnnotations(task).entrySet()) {
                for (Annotation annotation : info.getValue()) {
                    if (annotation.annotationType() == Command.class) {
                        Method method = info.getKey();

                        // compute command name
                        String commnad = method.getName().toLowerCase();

                        // register
                        commands.put(commnad, method);

                        if (!commnad.equals("help")) {
                            descriptions.put(commnad, ((Command) annotation).value());
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
        }
    }
}