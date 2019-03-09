/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.api;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import bee.Fail;
import bee.Platform;
import bee.UserInterface;
import bee.api.Task.Lifestyle;
import bee.util.Inputs;
import bee.util.lambda.ReflectableConsumer;
import bee.util.lambda.ReflectableFunction;
import kiss.Extensible;
import kiss.I;
import kiss.Manageable;
import kiss.Singleton;
import kiss.XML;
import kiss.model.Model;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.matcher.ElementMatchers;
import psychopath.Directory;
import psychopath.File;

/**
 * @version 2017/03/04 13:26:53
 */
@Manageable(lifestyle = Lifestyle.class)
public abstract class Task implements Extensible {

    /** The common task repository. */
    private static Map<String, Info> commons;

    /** The current processing project. */
    protected final Project project = I.make(Project.class);

    /** The user interface. */
    protected UserInterface ui = I.make(UserInterface.class);

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
        Info info = info(computeTaskName(getClass()));

        for (Entry<String, String> entry : info.descriptions.entrySet()) {
            // display usage description for this command
            ui.talk(entry.getKey(), " - ", entry.getValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Model.of(this).name;
    }

    /**
     * Use other tasks.
     * 
     * @param task
     */
    protected final <T extends Task, R> R require(ReflectableFunction<T, R> task) {
        return I.signal(task).joinAll(t -> {
            T instance = (T) I.make(info(computeTaskName(t.clazz())).task);

            return task.apply(instance);
        }).to().v;
    }

    /**
     * Use other tasks.
     * 
     * @param task
     */
    protected final <T extends Task> T require(ReflectableConsumer<T> task) {
        return I.signal(task).joinAll(t -> {
            T instance = (T) I.make(info(computeTaskName(t.clazz())).task);

            task.accept(instance);
            return instance;
        }).to().v;
    }

    /**
     * Use other tasks.
     * 
     * @param tasks
     */
    protected final <T extends Task> void require(ReflectableConsumer<? super T>... tasks) {
        LinkedList<ParallelUI> list = new LinkedList();

        I.signal(tasks).joinAll(task -> {
            ParallelUI ui = new ParallelUI();
            list.add(ui);

            T instance = (T) I.make(info(computeTaskName(task.clazz())).task);
            instance.ui = ui;

            task.accept(instance);

            return null;
        }).to(I.NoOP);

        for (ParallelUI ui : list) {
            ui.clear();
        }
    }

    /**
     * Use other tasks.
     * 
     * @param tasks
     */
    protected final <T1 extends Task, T2 extends Task> void require2(ReflectableConsumer<T1> task1, ReflectableConsumer<T2> task2) {
        LinkedList<ParallelUI> list = new LinkedList();

        I.signal(task1, task2).joinAll(task -> {
            ParallelUI ui = new ParallelUI();
            list.add(ui);

            Task instance = I.make(info(computeTaskName(task.clazz())).task);
            instance.ui = ui;

            task.accept(instance);

            return null;
        }).to(I.NoOP);

        for (ParallelUI ui : list) {
            ui.clear();
        }
    }

    /**
     * 
     */
    private class ParallelUI extends UserInterface {

        private Queue<Runnable> messages = new LinkedList();

        private ParallelUI next;

        /**
         * {@inheritDoc}
         */
        @Override
        public synchronized void write(String message) {
            if (message == null) {
                ui.write(message);
            } else {
                messages.add(() -> ui.write(message));
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Appendable getInterface() {
            return ui.getInterface();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public synchronized void startCommand(String name, Command command) {
            if (messages == null) {
                ui.startCommand(name, command);
            } else {
                messages.add(() -> ui.startCommand(name, command));
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public synchronized void endCommand(String name, Command command) {
            if (messages == null) {
                ui.endCommand(name, command);
            } else {
                messages.add(() -> ui.endCommand(name, command));
            }
        }

        private synchronized void clear() {
            while (messages.isEmpty() == false) {
                messages.poll().run();
            }
            messages = null;
        }
    }

    /**
     * <p>
     * Use other task from literal task expression.
     * </p>
     * 
     * @param tasks A list of tasks.
     */
    protected final void require(String... tasks) {
        for (String task : tasks) {
            execute(task);
        }
    }

    /**
     * <p>
     * Utility method for task.
     * </p>
     * 
     * @param path
     */
    protected final Path makeDirectory(Path path) {
        if (path != null && Files.notExists(path)) {
            try {
                Files.createDirectories(path);

                ui.talk("Make directory [" + path.toAbsolutePath() + "]");
            } catch (IOException e) {
                throw I.quiet(e);
            }
        }
        return path;
    }

    /**
     * <p>
     * Utility method for task.
     * </p>
     * 
     * @param directory
     */
    protected final Directory makeDirectory(Directory directory) {
        if (directory != null && directory.isAbsent()) {
            directory.create();

            ui.talk("Make directory [" + directory.absolutize() + "]");
        }
        return directory;
    }

    /**
     * <p>
     * Utility method for task.
     * </p>
     * 
     * @param path
     */
    protected final Path makeDirectory(Path base, String path) {
        return makeDirectory(base.resolve(path));
    }

    /**
     * <p>
     * Utility method for task.
     * </p>
     * 
     * @param path
     */
    protected final Directory makeDirectory(Directory base, String path) {
        return makeDirectory(base.directory(path));
    }

    /**
     * <p>
     * Utility method to write xml file.
     * </p>
     * 
     * @param path A file path to write.
     * @param xml A file contents.
     */
    protected final Path makeFile(Path path, XML xml) {
        makeDirectory(path.getParent());

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            xml.to(writer);

            ui.talk("Make file [" + path.toAbsolutePath() + "]");
        } catch (IOException e) {
            throw I.quiet(e);
        }
        return path;
    }

    /**
     * <p>
     * Utility method to write xml file.
     * </p>
     * 
     * @param file A file path to write.
     * @param xml A file contents.
     */
    protected final File makeFile(File file, XML xml) {
        makeDirectory(file.parent());

        try (BufferedWriter writer = file.newBufferedWriter()) {
            xml.to(writer);

            ui.talk("Make file [" + file.absolutize() + "]");
        } catch (IOException e) {
            throw I.quiet(e);
        }
        return file;
    }

    /**
     * <p>
     * Utility method to write property file.
     * </p>
     * 
     * @param path A file path to write.
     * @param properties A file contents.
     */
    protected final Path makeFile(Path path, Properties properties) {
        makeDirectory(path.getParent());

        try {
            properties.store(Files.newOutputStream(path), "");

            ui.talk("Make file [" + path.toAbsolutePath() + "]");
        } catch (IOException e) {
            throw I.quiet(e);
        }
        return path;
    }

    /**
     * <p>
     * Utility method to write file.
     * </p>
     * 
     * @param path A file path to write.
     * @param content A file content.
     */
    protected final Path makeFile(Path path, String content) {
        return makeFile(path, Arrays.asList(content.split(Platform.EOL)));
    }

    /**
     * <p>
     * Utility method to write file.
     * </p>
     * 
     * @param file A file path to write.
     * @param content A file content.
     */
    protected final File makeFile(File file, String content) {
        return makeFile(file, Arrays.asList(content.split(Platform.EOL)));
    }

    /**
     * <p>
     * Utility method to write file.
     * </p>
     * 
     * @param path A file path to write.
     * @param content A file content.
     */
    protected final Path makeFile(Path path, Iterable<String> content) {
        makeDirectory(path.getParent());

        try {
            Files.write(path, content, StandardCharsets.UTF_8);

            ui.talk("Make file [" + path.toAbsolutePath() + "]");
        } catch (IOException e) {
            throw I.quiet(e);
        }
        return path;
    }

    /**
     * <p>
     * Utility method to write file.
     * </p>
     * 
     * @param file A file path to write.
     * @param content A file content.
     */
    protected final File makeFile(File file, Iterable<String> content) {
        makeDirectory(file.parent());

        file.text(content);
        ui.talk("Make file [" + file.absolutize() + "]");

        return file;
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
     * <p>
     * Execute literal expression task.
     * </p>
     * 
     * @param input User task input.
     */
    private static final void execute(String input) {
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
            Fail failure = new Fail("Task [" + taskName + "] doesn't have the command [" + commandName + "]. Task [" + taskName + "] can use the following commands.");
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
    private static final Info info(String name) {
        if (name == null) {
            throw new Error("You must specify task name.");
        }

        if (commons == null) {
            commons = new TreeMap();

            for (Class<Task> task : I.findAs(Task.class)) {
                String taskName = computeTaskName(task);
                commons.put(taskName, new Info(taskName, task));
            }
        }

        // search from common tasks
        Info info = commons.get(name);

        if (info == null) {
            Fail failure = new Fail("Task [" + name + "] is not found. You can use the following tasks.");
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

    /**
     * @version 2018/03/29 11:50:35
     */
    static class Lifestyle extends kiss.Prototype<Object> {

        private static final Interceptor interceptor = I.make(Interceptor.class);

        /**
         * @param modelClass
         */
        public Lifestyle(Class modelClass) {
            super(new ByteBuddy().subclass(modelClass)
                    .method(ElementMatchers.any())
                    .intercept(MethodDelegation.to(interceptor))
                    .make()
                    .load(Thread.currentThread().getContextClassLoader())
                    .getLoaded());
        }
    }

    /**
     * @version 2018/03/29 11:50:31
     */
    @Manageable(lifestyle = Singleton.class)
    protected static class Interceptor {

        /** The executed commands results. */
        private final Map<String, Object> results = new HashMap();

        /** The user interface. */
        private final UserInterface ui;

        /**
         * @param ui
         */
        private Interceptor(UserInterface ui) {
            this.ui = ui;
        }

        @RuntimeType
        public Object intercept(@SuperCall Callable<?> zuper, @Origin Method method) throws Exception {
            Command command = find(method);

            if (command == null) {
                return zuper.call();
            }

            String name = Inputs.hyphenize(method.getDeclaringClass().getSimpleName()) + ":" + method.getName();

            Object result = results.get(name);

            if (!results.containsKey(name)) {
                ui.startCommand(name, command);
                result = zuper.call();
                ui.endCommand(name, command);

                results.put(name, result);
            }
            return result;
        }

        /**
         * <p>
         * Find command annotaton.
         * </p>
         * 
         * @param method
         * @return
         */
        private Command find(Method method) {
            Class clazz = method.getDeclaringClass();

            while (clazz != Object.class) {
                for (Method m : clazz.getDeclaredMethods()) {
                    if (m.getName().contentEquals(method.getName()) && Arrays
                            .deepEquals(m.getParameterTypes(), method.getParameterTypes())) {
                        Command command = m.getAnnotation(Command.class);

                        if (command != null) {
                            return command;
                        }
                    }
                }
                clazz = clazz.getSuperclass();
            }
            return null;
        }
    }
}
