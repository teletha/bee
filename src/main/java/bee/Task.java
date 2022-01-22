/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee;

import static org.objectweb.asm.Opcodes.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import bee.Task.TaskLifestyle;
import bee.api.Command;
import bee.api.Project;
import bee.util.EnhancedClassWriter;
import bee.util.EnhancedMethodWriter;
import bee.util.Inputs;
import bee.util.Profiling;
import kiss.Extensible;
import kiss.I;
import kiss.Lifestyle;
import kiss.Managed;
import kiss.WiseFunction;
import kiss.XML;
import kiss.model.Model;
import psychopath.Directory;
import psychopath.File;

@Managed(value = TaskLifestyle.class)
public abstract class Task implements Extensible {

    /** The common task repository. */
    static Map<String, Info> commons;

    /** The current processing project. */
    protected final Project project = I.make(Project.class);

    /** The user interface. */
    protected UserInterface ui = I.make(UserInterface.class);

    /**
     * Execute manual tasks.
     */
    public void execute() {
        // do nothing
    }

    @Command("Display help message for all commands of this task.")
    public void help() {
        Info info = info(computeTaskName(getClass()));

        for (Entry<String, String> entry : info.descriptions.entrySet()) {
            // display usage description for this command
            ui.info(entry.getKey(), " - ", entry.getValue());
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
     * Execute required tasks.
     * 
     * @param task A task to execute.
     */
    protected final <T extends Task, R> R require(ValuedTaskRef<T, R> task) {
        return (R) requireParallel(new TaskRef[] {task});
    }

    /**
     * Execute required tasks.
     * 
     * @param task A task to execute.
     */
    protected final <T extends Task> void require(TaskRef<T> task) {
        requireParallel(new TaskRef[] {task});
    }

    /**
     * Execute required tasks in parallel.
     * 
     * @param task1 A task to execute.
     * @param task2 A task to execute.
     */
    protected final <T1 extends Task, T2 extends Task> void require(TaskRef<T1> task1, TaskRef<T2> task2) {
        requireParallel(new TaskRef[] {task1, task2});
    }

    /**
     * Execute required tasks in parallel.
     * 
     * @param task1 A task to execute.
     * @param task2 A task to execute.
     * @param task3 A task to execute.
     */
    protected final <T1 extends Task, T2 extends Task, T3 extends Task> void require(TaskRef<T1> task1, TaskRef<T2> task2, TaskRef<T3> task3) {
        requireParallel(new TaskRef[] {task1, task2, task3});
    }

    /**
     * Execute required tasks in parallel.
     * 
     * @param task1 A task to execute.
     * @param task2 A task to execute.
     * @param task3 A task to execute.
     * @param task4 A task to execute.
     */
    protected final <T1 extends Task, T2 extends Task, T3 extends Task, T4 extends Task> void require(TaskRef<T1> task1, TaskRef<T2> task2, TaskRef<T3> task3, TaskRef<T4> task4) {
        requireParallel(new TaskRef[] {task1, task2, task3, task4});
    }

    /**
     * Execute required tasks in parallel.
     * 
     * @param task1 A task to execute.
     * @param task2 A task to execute.
     * @param task3 A task to execute.
     * @param task4 A task to execute.
     * @param task5 A task to execute.
     */
    protected final <T1 extends Task, T2 extends Task, T3 extends Task, T4 extends Task, T5 extends Task> void require(TaskRef<T1> task1, TaskRef<T2> task2, TaskRef<T3> task3, TaskRef<T4> task4, TaskRef<T5> task5) {
        requireParallel(new TaskRef[] {task1, task2, task3, task4, task5});
    }

    /**
     * Use other tasks.
     * 
     * @param tasks
     */
    private Object requireParallel(TaskRef<Task>[] tasks) {
        ConcurrentLinkedDeque<ParallelInterface> parallels = new ConcurrentLinkedDeque();
        ParallelInterface parallel = null;

        for (int i = 0; i < tasks.length; i++) {
            parallels.offerFirst(parallel = new ParallelInterface(ui, parallel));
        }
        parallels.peekFirst().start();

        return I.signal(tasks).joinAll(task -> {
            LifestyleForProject.local.set(project);

            Method m = task.getClass().getDeclaredMethod("writeReplace");
            m.setAccessible(true);

            SerializedLambda s = (SerializedLambda) m.invoke(task);
            Method method = I.type(s.getImplClass().replaceAll("/", ".")).getMethod(s.getImplMethodName());

            return execute(computeTaskName(method.getDeclaringClass()) + ":" + method.getName().toLowerCase(), parallels.pollFirst());
        }).to().v;
    }

    /**
     * 
     */
    private static class ParallelInterface extends UserInterface {

        /** The message mode. */
        // buffering(0) → buffered(2)
        // ↓
        // processing(1)
        private int mode = 0;

        /** The message buffer. */
        private Queue<Runnable> messages = new LinkedList();

        /** The original ui. */
        private final UserInterface ui;

        /** The next task's interface. */
        private final ParallelInterface next;

        /**
         * @param next
         */
        private ParallelInterface(UserInterface ui, ParallelInterface next) {
            this.ui = ui;
            this.next = next;
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
        protected synchronized void write(int type, String message) {
            switch (mode) {
            case 0:
                messages.add(() -> ui.write(type, message));
                break;

            case 1:
                ui.write(type, message);
                break;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void write(Throwable error) {
            switch (mode) {
            case 0:
                messages.add(() -> ui.write(error));
                break;

            case 1:
                ui.write(error);
                break;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected synchronized void startCommand(String name, Command command) {
            switch (mode) {
            case 0:
                messages.add(() -> ui.startCommand(name, command));
                break;

            case 1:
                ui.startCommand(name, command);
                break;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected synchronized void endCommand(String name, Command command) {
            switch (mode) {
            case 0:
                messages.add(() -> ui.endCommand(name, command));
                break;

            case 1:
                ui.endCommand(name, command);
                break;
            }
        }

        /**
         * Invoke when the task was finished.
         */
        private synchronized void finish() {
            switch (mode) {
            case 0: // buffering
                mode = 2;
                break;

            case 1: // processing
                if (next != null) next.start();
                break;

            case 2: // buffered
                break;
            }
        }

        /**
         * Invoke when the task is processing.
         */
        private synchronized void start() {
            switch (mode) {
            case 0: // buffering
                mode = 1;
                while (!messages.isEmpty()) {
                    messages.poll().run();
                }
                break;

            case 1: // processing
                break;

            case 2: // buffered
                while (!messages.isEmpty()) {
                    messages.poll().run();
                }
                if (next != null) next.start();
                break;
            }
        }
    }

    /**
     * Utility method for task.
     * 
     * @param path
     */
    protected final Directory makeDirectory(Directory base, String path) {
        Directory directory = base.directory(path);

        if (directory.isAbsent()) {
            directory.create();

            ui.info("Make directory [", directory.absolutize(), "]");
        }
        return directory;
    }

    /**
     * Utility method to write xml file.
     * 
     * @param file A file path to write.
     * @param xml A file contents.
     */
    protected final File makeFile(File file, XML xml) {
        try (BufferedWriter writer = file.newBufferedWriter()) {
            xml.to(writer);

            ui.info("Make file [", file.absolutize(), "]");
        } catch (IOException e) {
            throw I.quiet(e);
        }
        return file;
    }

    /**
     * Utility method to write property file.
     * 
     * @param path A file path to write.
     * @param properties A file contents.
     */
    protected final File makeFile(File path, Properties properties) {
        path.parent().create();

        try {
            properties.store(path.newOutputStream(), "");

            ui.info("Make file [", path.absolutize(), "]");
        } catch (IOException e) {
            throw I.quiet(e);
        }
        return path;
    }

    /**
     * Utility method to write file.
     * 
     * @param path A file path to write.
     * @param content A file content.
     */
    protected final File makeFile(String path, String content) {
        if (path == null) {
            throw new Fail("Input file is null.");
        }
        return makeFile(project.getRoot().file(path), content);
    }

    /**
     * Utility method to write file.
     * 
     * @param file A file path to write.
     * @param content A file content.
     */
    protected final File makeFile(File file, String content) {
        return makeFile(file, Arrays.asList(content.split("\\R")));
    }

    /**
     * Utility method to write file.
     * 
     * @param path A file path to write.
     * @param content A file content.
     */
    protected final File makeFile(String path, Iterable<String> content) {
        if (path == null) {
            throw new Fail("Input file is null.");
        }
        return makeFile(project.getRoot().file(path), content);
    }

    /**
     * Utility method to write file.
     * 
     * @param file A file path to write.
     * @param content A file content.
     */
    protected final File makeFile(File file, Iterable<String> content) {
        if (file == null) {
            throw new Fail("Input file is null.");
        }

        file.text(content);

        Iterator<String> iterator = content.iterator();
        if (iterator.hasNext() && iterator.next().startsWith("#!")) {
            file.text(x -> x.replaceAll("\\R", "\n"));
        }

        ui.info("Make file [", file.absolutize(), "]");

        return file;
    }

    /**
     * Utility method to write file.
     * 
     * @param file A file path to write.
     * @param replacer A file content replacer.
     */
    protected final File makeFile(File file, WiseFunction<String, String> replacer) {
        return makeFile(file, file.lines().map(replacer).toList());
    }

    /**
     * Utility method to delete file.
     * 
     * @param path A file path to delete.
     */
    protected final void deleteFile(String path) {
        if (path != null) {
            deleteFile(project.getRoot().file(path));
        }
    }

    /**
     * Utility method to delete file.
     * 
     * @param file A file to delete.
     */
    protected final void deleteFile(File file) {
        if (file != null && file.isPresent()) {
            file.delete();
            ui.info("Delete file [", file.absolutize(), "]");
        }
    }

    /**
     * Utility method to delete file.
     * 
     * @param from A file to copy.
     * @param to A destination.
     */
    protected final void copyFile(File from, File to) {
        if (from == null) {
            throw new Fail("The specified file is null.");
        }

        if (from.isAbsent()) {
            throw new Fail("File [" + from + "] is not found.");
        }

        from.copyTo(to);
        ui.info("Copy file from [", from.absolutize(), "] to [", to.absolutize() + "]");
    }

    /**
     * Utility method to check file.
     * 
     * @param path A file path to check.
     */
    protected final boolean checkFile(String path) {
        if (path == null) {
            return false;
        }
        return checkFile(project.getRoot().file(path));
    }

    /**
     * Utility method to check file.
     * 
     * @param file A file to check.
     */
    protected final boolean checkFile(File file) {
        return file != null && file.isPresent();
    }

    /**
     * Execute literal expression task.
     * 
     * @param input User task for input.
     * @param ui User interface for output.
     */
    final Object execute(String input, UserInterface ui) {
        // parse command
        if (input == null) {
            return null;
        }

        // remove head and tail white space
        input = input.trim();

        if (input.length() == 0) {
            return null;
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
        commandName = commandName.toLowerCase();

        // search command
        Method command = info.commands.get(commandName);

        if (command == null) {
            // Search for command with similar names for possible misspellings.
            String recommend = Inputs.recommend(commandName, info.commands.keySet());
            if (recommend != null && ui.confirm("Isn't it a misspelling of command [" + recommend + "] ?")) {
                command = info.commands.get(recommend);
            } else {
                Fail failure = new Fail("Task [" + taskName + "] doesn't have the command [" + commandName + "]. Task [" + taskName + "] can use the following commands.");
                for (Entry<String, String> entry : info.descriptions.entrySet()) {
                    failure.solve(String.format("%s:%-8s \t%s", taskName, entry.getKey(), entry.getValue()));
                }
                throw failure;
            }
        }

        String fullname = taskName + ":" + commandName;

        // skip option
        if (BeeOption.Skip.value.contains(taskName) || BeeOption.Skip.value.contains(fullname)) {
            return null;
        }

        // create task and initialize
        Task task = I.make(info.task);
        task.ui = ui;

        // execute task
        try (var x = Profiling.of("Task [" + fullname + "]")) {
            return command.invoke(task);
        } catch (TaskCancel e) {
            ui.warn("The task [", fullname, "] was canceled beacuase ", e.getMessage());
            return null;
        } catch (Throwable e) {
            if (e instanceof InvocationTargetException) {
                e = ((InvocationTargetException) e).getTargetException();
            }
            throw I.quiet(e);
        } finally {
            if (ui instanceof ParallelInterface) {
                ((ParallelInterface) ui).finish();
            }
        }
    }

    /**
     * Compute human-readable task name.
     * 
     * @param taskClass A target task.
     * @return A task name.
     */
    private final String computeTaskName(Class taskClass) {
        if (taskClass.isSynthetic()) {
            return computeTaskName(taskClass.getSuperclass());
        }
        return Inputs.hyphenize(taskClass.getSimpleName());
    }

    /**
     * Find task information by name.
     * 
     * @param name A task name.
     * @return A specified task.
     */
    private final Info info(String name) {
        if (name == null) {
            throw new Error("You must specify task name.");
        }

        synchronized (Task.class) {
            if (commons == null) {
                commons = new TreeMap();

                for (Class<Task> task : I.findAs(Task.class)) {
                    String taskName = computeTaskName(task);
                    Info info = new Info(taskName, task);
                    if (!info.descriptions.isEmpty()) {
                        commons.put(taskName, info);
                    }
                }
            }
        }

        // search from common tasks
        Info info = commons.get(name);

        if (info == null) {
            // Search for tasks with similar names for possible misspellings.
            String recommend = Inputs.recommend(name, commons.keySet());
            if (recommend != null && ui.confirm("Isn't it a misspelling of task [" + recommend + "] ?")) {
                return commons.get(recommend);
            }

            Fail failure = new Fail("Task [" + name + "] is not found. You can use the following tasks.");
            for (Info i : commons.values()) {
                failure.solve(i);
            }
            throw failure;
        }

        // API definition
        return info;
    }

    /**
     * 
     */
    private static final class Info {

        /** The task name. */
        private final String name;

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
            this.name = name;
            this.task = task;

            for (Entry<Method, List<Annotation>> info : Model.collectAnnotatedMethods(task).entrySet()) {
                for (Annotation annotation : info.getValue()) {
                    if (annotation.annotationType() == Command.class) {
                        Method method = info.getKey();

                        // compute command name
                        Command command = (Command) annotation;
                        String commnadName = Inputs.hyphenize(method.getName());

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

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return String.format("%-8s \t%s", name, descriptions.get(defaultCommnad));
        }
    }

    /**
     * Store for each command result.
     */
    @SuppressWarnings("serial")
    private static class Cache extends HashMap<String, Object> {
    }

    // private static class ForASMifier extends Jar {
    //
    // @Override
    // public void source() {
    // String name = Inputs.hyphenize("Jar:source");
    // Map<String, Object> results = project.associate(Cache.class);
    // Object o = results.get(name);
    // if (!results.containsKey(name)) {
    // ui.startCommand(name, null);
    // super.source();
    // ui.endCommand(name, null);
    //
    // results.put(name, o);
    // }
    // }
    // }

    /**
     * 
     */
    static class TaskLifestyle implements Lifestyle<Object> {

        private final Lifestyle lifestyle;

        /**
         * @param model
         */
        public TaskLifestyle(Class<?> model) {
            lifestyle = I.prototype(EnhancedClassWriter.define(Task.class, "Memoized" + model.getSimpleName(), writer -> {
                // ======================================
                // Define and build the memoized task class
                // ======================================
                String task = Type.getInternalName(Task.class);
                String parent = Type.getInternalName(model);
                writer.visit(V16, ACC_PUBLIC | ACC_SUPER, writer.classInternalName, null, parent, null);

                // constructor
                EnhancedMethodWriter mw = writer.writeMethod(ACC_PUBLIC, "<init>", "()V", null, null);
                mw.visitVarInsn(ALOAD, 0);
                mw.visitMethodInsn(INVOKESPECIAL, parent, "<init>", "()V", false);
                mw.visitInsn(RETURN);
                mw.visitMaxs(1, 1);
                mw.visitEnd();

                // overwrite command methods
                for (Method m : model.getMethods()) {
                    if (MethodUtils.getAnnotation(m, Command.class, true, true) != null) {
                        String methodName = m.getName();
                        String methodDesc = Type.getMethodDescriptor(m);
                        Type returnType = Type.getReturnType(m);
                        boolean valued = m.getReturnType() != void.class;

                        mw = writer.writeMethod(ACC_PUBLIC, methodName, methodDesc, null, null);
                        mw.visitLdcInsn(model.getSimpleName() + ":" + Inputs.hyphenize(methodName));
                        mw.visitMethodInsn(INVOKESTATIC, "bee/util/Inputs", "hyphenize", "(Ljava/lang/String;)Ljava/lang/String;", false);
                        mw.visitVarInsn(ASTORE, 1);

                        mw.visitVarInsn(ALOAD, 0);
                        mw.visitFieldInsn(GETFIELD, task, "project", "Lbee/api/Project;");
                        mw.visitLdcInsn(Type.getType(Cache.class));
                        mw.visitMethodInsn(INVOKEVIRTUAL, "bee/api/Project", "associate", "(Ljava/lang/Class;)Ljava/lang/Object;", false);
                        mw.visitTypeInsn(CHECKCAST, "java/util/Map");
                        mw.visitVarInsn(ASTORE, 2);

                        mw.visitVarInsn(ALOAD, 2);
                        mw.visitVarInsn(ALOAD, 1);
                        mw.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
                        mw.visitVarInsn(ASTORE, 3);

                        mw.visitVarInsn(ALOAD, 2);
                        mw.visitVarInsn(ALOAD, 1);
                        mw.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "containsKey", "(Ljava/lang/Object;)Z", true);

                        Label label3 = new Label();
                        mw.visitJumpInsn(IFNE, label3);
                        mw.visitVarInsn(ALOAD, 0);
                        mw.visitFieldInsn(GETFIELD, parent, "ui", "Lbee/UserInterface;");

                        mw.visitVarInsn(ALOAD, 1);
                        mw.visitInsn(ACONST_NULL);
                        mw.visitMethodInsn(INVOKEVIRTUAL, "bee/UserInterface", "startCommand", "(Ljava/lang/String;Lbee/api/Command;)V", false);
                        mw.visitVarInsn(ALOAD, 0);
                        mw.visitMethodInsn(INVOKESPECIAL, parent, methodName, methodDesc, false);
                        if (valued) {
                            mw.wrap(returnType);
                            mw.visitVarInsn(ASTORE, 3);
                        }

                        mw.visitVarInsn(ALOAD, 0);
                        mw.visitFieldInsn(GETFIELD, parent, "ui", "Lbee/UserInterface;");
                        mw.visitVarInsn(ALOAD, 1);
                        mw.visitInsn(ACONST_NULL);
                        mw.visitMethodInsn(INVOKEVIRTUAL, "bee/UserInterface", "endCommand", "(Ljava/lang/String;Lbee/api/Command;)V", false);

                        mw.visitVarInsn(ALOAD, 2);
                        mw.visitVarInsn(ALOAD, 1);
                        mw.visitVarInsn(ALOAD, 3);
                        mw.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
                        mw.visitInsn(POP);

                        mw.visitLabel(label3);
                        if (valued) {
                            mw.visitVarInsn(ALOAD, 3);
                            mw.unwrap(returnType);
                            mw.visitInsn(returnType.getOpcode(IRETURN));
                        } else {
                            mw.visitInsn(RETURN);
                        }
                        mw.visitMaxs(0, 0);
                        mw.visitEnd();
                    }
                }
                writer.visitEnd();
            }));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object call() {
            return lifestyle.get();
        }
    }

    /**
     * 
     */
    public interface TaskRef<T> extends Consumer<T>, Serializable {
    }

    /**
     * 
     */
    public interface ValuedTaskRef<T, R> extends Function<T, R>, Serializable, TaskRef<T> {

        @Override
        default void accept(T task) {
            apply(task);
        }
    }
}