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
import bee.definition.Project;

/**
 * @version 2012/03/20 16:27:09
 */
@Manageable(lifestyle = Singleton.class)
public class Tasks implements ClassListener<Task> {

    /** The task repository. */
    private static Map<String, TaskInfo> infos = new HashMap();

    /**
     * {@inheritDoc}
     */
    @Override
    public void load(Class<Task> clazz) {
        infos.put(clazz.getSimpleName().toLowerCase(), new TaskInfo(clazz));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unload(Class<Task> clazz) {
        infos.remove(clazz.getSimpleName().toLowerCase());
    }

    /**
     * <p>
     * Execute command.
     * </p>
     * 
     * @param project
     * @param input
     */
    public static final void execute(Project project, String input, UserInterface ui) {
        // parse command
        if (input == null) {
            return;
        }

        // remove head and tail white space
        input = input.trim();

        if (input.length() == 0) {
            return;
        }

        // search task name
        String taskName;
        int index = input.indexOf(' ');

        if (index == -1) {
            taskName = input;
        } else {
            taskName = input.substring(0, index);
            input = input.substring(index + 1);
        }

        // analyze task name
        String taskGroupName = "";
        String commandName = "";
        index = taskName.indexOf(':');

        if (index == -1) {
            taskGroupName = taskName;
        } else {
            taskGroupName = taskName.substring(0, index);
            commandName = taskName.substring(index + 1);
        }

        // search task
        TaskInfo taskInfo = infos.get(taskGroupName.toLowerCase());

        if (taskInfo == null) {
            ui.error("Task '" + taskName + "' is not found.");
            return;
        }

        if (commandName.length() == 0) {
            commandName = taskInfo.defaults;
        }

        // search command
        Method command = taskInfo.infos.get(commandName.toLowerCase());

        if (command == null) {
            return;
        }

        // create task and initialize
        Task task = I.make(taskInfo.task);

        // execute task
        try {
            command.invoke(task);
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    /**
     * @version 2012/03/20 16:29:25
     */
    private static class TaskInfo {

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
