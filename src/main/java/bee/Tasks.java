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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import kiss.ClassListener;
import kiss.Manageable;
import kiss.Singleton;
import bee.project.Project;
import bee.task.Command;

/**
 * @version 2012/03/20 16:27:09
 */
@Manageable(lifestyle = Singleton.class)
class Tasks implements ClassListener<Task> {

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
    public static final void execute(Project project, String input) {
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
        String task;
        int index = input.indexOf(' ');

        if (index == -1) {
            task = input;
        } else {
            task = input.substring(0, index);
            input = input.substring(index + 1);
        }

        // analyze task name
        String taskName = "";
        String commandName = "";
        index = task.indexOf(':');

        if (index == -1) {
            taskName = task;
        } else {
            taskName = task.substring(0, index);
            commandName = task.substring(index + 1);
        }

        // search task
        TaskInfo taskInfo = infos.get(taskName.toLowerCase());

        if (taskInfo == null) {
            return;
        }

        if (commandName.length() == 0) {
            commandName = taskInfo.defaultCommandName;
        }

        // search command
        CommandInfo commandInfo = taskInfo.infos.get(commandName.toLowerCase());

        if (commandInfo == null) {
            return;
        }

    }

    /**
     * @version 2012/03/20 16:29:25
     */
    private static class TaskInfo {

        /** The task definition. */
        private final Class<Task> taskClass;

        /** The default command name. */
        private String defaultCommandName;

        /** The command infos. */
        private Map<String, CommandInfo> infos = new HashMap();

        /**
         * @param taskClass
         */
        private TaskInfo(Class<Task> taskClass) {
            this.taskClass = taskClass;

            for (Method method : taskClass.getDeclaredMethods()) {
                Command command = method.getAnnotation(Command.class);

                if (command != null) {
                    String name = method.getName().toLowerCase();

                    infos.put(name, new CommandInfo(method));
                    defaultCommandName = name;
                }
            }

        }

        /**
         * <p>
         * Execute task
         * </p>
         * 
         * @param commandName
         */
        private void execute(String commandName) {

        }
    }

    /**
     * @version 2012/03/20 16:47:54
     */
    private static class CommandInfo {

        /** The actual command. */
        private final Method method;

        /**
         * @param method
         */
        private CommandInfo(Method method) {
            this.method = method;
        }
    }
}
