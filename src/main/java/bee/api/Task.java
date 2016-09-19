/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.api;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;

import bee.Platform;
import bee.TaskFailure;
import bee.UserInterface;
import bee.util.Inputs;
import kiss.ClassListener;
import kiss.Extensible;
import kiss.I;
import kiss.Manageable;
import kiss.Singleton;
import kiss.XML;
import kiss.model.Model;

/**
 * @version 2015/06/22 13:17:10
 */
public abstract class Task implements Extensible {

    /** The current processing project. */
    protected final Project project = I.make(Project.class);

    /** The user interface. */
    protected final UserInterface ui = I.make(UserInterface.class);

    /**
     * <p>
     * Execute manual tasks.
     * </p>
     * 
     * @param tasks
     */
    public void execute() {
        // do nothing
    }

    @Command("Display help message for all commands of this task.")
    public void help() {
        Info info = I.make(Tasks.class).info(computeTaskName(getClass()));

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
        if (taskClass == null) {
            throw new Error("You must specify task class.");
        }
        return (T) I.make(I.make(Tasks.class).info(computeTaskName(taskClass)).task);
    }

    /**
     * <p>
     * Use other task from literal task expression.
     * </p>
     * 
     * @param taskClass A task class.
     * @return A target task.
     */
    protected final void require(String... tasks) {
        for (String task : tasks) {
            I.make(Tasks.class).execute(task);
        }
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

    /**
     * <p>
     * Utility method to write xml file.
     * </p>
     * 
     * @param path A file path to write.
     * @param xml A file contents.
     */
    protected final void makeFile(Path path, XML xml) {
        makeDirectory(path.getParent());

        try {
            xml.to(Files.newBufferedWriter(path, StandardCharsets.UTF_8));

            ui.talk("Make file [" + path.toAbsolutePath() + "]");
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * Utility method to write property file.
     * </p>
     * 
     * @param path A file path to write.
     * @param properties A file contents.
     */
    protected final void makeFile(Path path, Properties properties) {
        makeDirectory(path.getParent());

        try {
            properties.store(Files.newOutputStream(path), "");

            ui.talk("Make file [" + path.toAbsolutePath() + "]");
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * Utility method to write file.
     * </p>
     * 
     * @param path A file path to write.
     * @param content A file content.
     */
    protected final void makeFile(Path path, String content) {
        makeFile(path, Arrays.asList(content.split(Platform.EOL)));
    }

    /**
     * <p>
     * Utility method to write file.
     * </p>
     * 
     * @param path A file path to write.
     * @param content A file content.
     */
    protected final void makeFile(Path path, Iterable<String> content) {
        makeDirectory(path.getParent());

        try {
            Files.write(path, content, StandardCharsets.UTF_8);

            ui.talk("Make file [" + path.toAbsolutePath() + "]");
        } catch (IOException e) {
            throw I.quiet(e);
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
    private static final String computeTaskName(Class taskClass) {
        if (taskClass.isSynthetic()) {
            return computeTaskName(taskClass.getSuperclass());
        }
        return Inputs.hyphenize(taskClass.getSimpleName());
    }

    /**
     * @version 2012/05/17 16:49:24
     */
    @Manageable(lifestyle = Singleton.class)
    private static final class Tasks implements ClassListener<Task> {

        /** The common task repository. */
        private final Map<String, Info> commons = new TreeMap();

        /**
         * <p>
         * Execute literal expression task.
         * </p>
         * 
         * @param input User task input.
         */
        private void execute(String input) {
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
            Info info = info(taskName);

            if (commandName.isEmpty()) {
                commandName = info.defaultCommnad;
            }

            // search command
            Method command = info.commands.get(commandName.toLowerCase());

            if (command == null) {
                TaskFailure failure = new TaskFailure("Task [", taskName, "] doesn't have the command [", commandName, "]. Task [", taskName, "] can use the following commands.");
                for (Entry<String, String> entry : info.descriptions.entrySet()) {
                    failure.solve(taskName, ":", entry.getKey(), " - ", entry.getValue());
                }
                throw failure;
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
                throw I.quiet(e);
            }
        }

        /**
         * <p>
         * Find task information by name.
         * </p>
         * 
         * @param name A task name.
         * @return A specified task.
         */
        private Info info(String name) {
            if (name == null) {
                throw new Error("You must specify task name.");
            }

            // search from common tasks
            Info info = commons.get(name);

            if (info == null) {
                TaskFailure failure = new TaskFailure("Task [", name, "] is not found. You can use the following tasks.");
                for (Entry<String, Info> entry : commons.entrySet()) {
                    info = entry.getValue();
                    failure.solve(entry.getKey(), " - ", info.descriptions.get(info.defaultCommnad));
                }
                throw failure;
            }

            // API definition
            return info;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void load(Class<Task> clazz) {
            if (clazz.isLocalClass() || clazz.isMemberClass() || clazz.isAnonymousClass()) {
                return;
            }

            String name = computeTaskName(clazz);
            commons.put(name, new Info(name, clazz));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void unload(Class<Task> clazz) {
            if (clazz.isLocalClass() || clazz.isMemberClass() || clazz.isAnonymousClass()) {
                return;
            }

            String name = computeTaskName(clazz);
            commons.remove(name);
        }
    }

    /**
     * @version 2012/05/17 14:55:28
     */
    private static final class Info {

        /** The task definition. */
        private final Class<Task> task;

        /** The default command name. */
        private String defaultCommnad = "help";

        /** The actual commands. */
        private final Map<String, Method> commands = new TreeMap();

        /** The command descriptions. */
        private final Map<String, String> descriptions = new TreeMap();

        /**
         * @param name
         * @param task
         */
        private Info(String name, Class<Task> task) {
            this.task = task;

            for (Entry<Method, List<Annotation>> info : Model.collectAnnotatedMethods(task).entrySet()) {
                for (Annotation annotation : info.getValue()) {
                    if (annotation.annotationType() == Command.class) {
                        Method method = info.getKey();

                        // compute command name
                        Command command = (Command) annotation;
                        String commnadName = method.getName().toLowerCase();

                        // register
                        commands.put(commnadName, method);

                        if (!commnadName.equals("help")) {
                            descriptions.put(commnadName, command.value());
                        }

                        if (command.defaults()) {
                            defaultCommnad = commnadName;
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
