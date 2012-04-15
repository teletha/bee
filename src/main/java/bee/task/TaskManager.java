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
import kiss.Table;
import kiss.model.ClassUtil;
import bee.UserInterface;
import bee.api.Project;

/**
 * @version 2012/04/15 14:13:34
 */
@Manageable(lifestyle = Singleton.class)
public final class TaskManager implements ClassListener<Task> {

    /** The common task repository. */
    private final Map<String, TaskInfo> commons = new HashMap();

    /** The project specific task repository. */
    private final Map<Path, Map<String, TaskInfo>> projects = new HashMap();

    /**
     * <p>
     * Execute task.
     * </p>
     * 
     * @param input
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
            commandName = info.defaults;
        }
    
        // search command
        Method command = info.infos.get(commandName.toLowerCase());
    
        if (command == null) {
            throw new Error("Task [" + taskName + "] doesn't has the coommand [" + commandName + "].");
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
        return (T) I.make(find(Task.computeTaskName(taskClass)).task);
    }

    /**
     * <p>
     * Find task by name.
     * </p>
     * 
     * @param name A task name.
     * @return A specified task.
     */
    private TaskInfo find(String name) {
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
        String name = Task.computeTaskName(clazz);
        Path archive = ClassUtil.getArchive(clazz);

        if (Files.isRegularFile(archive)) {
            // common task
            commons.put(name, new TaskInfo(clazz));
        } else {
            // project specified task
            Map<String, TaskInfo> infos = projects.get(archive);

            if (infos == null) {
                infos = new HashMap();
                projects.put(archive, infos);
            }
            infos.put(name, new TaskInfo(clazz));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unload(Class<Task> clazz) {
        String name = Task.computeTaskName(clazz);
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
     * @version 2012/04/15 14:13:26
     */
    private static final class TaskInfo {

        /** The task definition. */
        private final Class<Task> task;

        /** The default command name. */
        private String defaults;

        /** The command infos. */
        private Map<String, Method> infos = new HashMap();

        /**
         * @param taskClass
         */
        private TaskInfo(Class<Task> taskClass) {
            this.task = taskClass;

            Table<Method, Annotation> methods = ClassUtil.getAnnotations(taskClass);

            for (Entry<Method, List<Annotation>> info : methods.entrySet()) {
                for (Annotation annotation : info.getValue()) {
                    if (annotation.annotationType() == Command.class) {
                        Command command = (Command) annotation;
                        Method method = info.getKey();

                        // compute command name
                        String name = method.getName().toLowerCase();

                        // register
                        infos.put(name, method);

                        // check default
                        if (command.defaults()) {
                            defaults = name;
                        }
                    }
                }
            }
        }
    }
}