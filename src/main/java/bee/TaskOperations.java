/*
 * Copyright (C) 2025 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import bee.Task.TaskReference;
import bee.Task.ValuedTaskReference;
import bee.api.Project;
import kiss.I;
import kiss.Model;
import kiss.WiseConsumer;
import kiss.WiseFunction;
import kiss.XML;
import psychopath.Directory;
import psychopath.File;
import psychopath.Option;

public class TaskOperations {

    /**
     * Get the current processing {@link Project}.
     * 
     * @return
     */
    public static final Project project() {
        return ForProject.local.get();
    }

    /**
     * Get the current {@link UserInterface}.
     * 
     * @return
     */
    public static final UserInterface ui() {
        return ForUI.local.get();
    }

    /**
     * Get the task config which is associated with the current project.
     */
    public static final <T extends Task<C>, C> C config(Class<T> task) {
        TaskInfo.by(task);

        Type[] types = Model.collectParameters(task, Task.class);
        if (types.length != 0) {
            return project().associate((Class<C>) types[0]);
        } else {
            throw new Fail(task + " doesn't have config class.");
        }
    }

    /**
     * Get the task config which is associated with the current project.
     */
    public static final <T extends Task<C>, C> void config(Class<T> task, WiseConsumer<C> config) {
        if (config != null) {
            config.accept(config(task));
        }
    }

    /**
     * Utility method for task.
     * 
     * @param path
     */
    public static final Directory makeDirectory(Directory base, String path) {
        Directory directory = base.directory(path);

        if (directory.isAbsent()) {
            directory.create();

            ui().info("Make directory [", directory.absolutize(), "]");
        }
        return directory;
    }

    /**
     * Utility method to write xml file.
     * 
     * @param file A file path to write.
     * @param xml A file contents.
     */
    public static final File makeFile(File file, XML xml) {
        try (BufferedWriter writer = file.newBufferedWriter()) {
            xml.to(writer);

            ui().info("Make file [", file.absolutize(), "]");
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
    public static final File makeFile(File path, Properties properties) {
        path.parent().create();

        try {
            properties.store(path.newOutputStream(), "");

            ui().info("Make file [", path.absolutize(), "]");
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
    public static final File makeFile(String path, String content) {
        if (path == null) {
            throw new Fail("Input file is null.");
        }
        return makeFile(project().getRoot().file(path), content);
    }

    /**
     * Utility method to write file.
     * 
     * @param file A file path to write.
     * @param content A file content.
     */
    public static final File makeFile(File file, String content) {
        return makeFile(file, Arrays.asList(content.split("\\R")));
    }

    /**
     * Utility method to write file.
     * 
     * @param path A file path to write.
     * @param content A file content.
     */
    public static final File makeFile(String path, Iterable<String> content) {
        if (path == null) {
            throw new Fail("Input file is null.");
        }
        return makeFile(project().getRoot().file(path), content);
    }

    /**
     * Utility method to write file.
     * 
     * @param file A file path to write.
     * @param content A file content.
     */
    public static final File makeFile(File file, Iterable<String> content) {
        if (file == null) {
            throw new Fail("Input file is null.");
        }

        file.text(content);

        Iterator<String> iterator = content.iterator();
        if (iterator.hasNext() && iterator.next().startsWith("#!")) {
            file.text(x -> x.replaceAll("\\R", "\n"));
        }

        ui().info("Make file [", file.absolutize(), "]");

        return file;
    }

    /**
     * Utility method to write file.
     * 
     * @param file A file path to write.
     * @param replacer A file content replacer.
     */
    public static final File makeFile(File file, WiseFunction<String, String> replacer) {
        return makeFile(file, file.lines().map(replacer).toList());
    }

    /**
     * Utility method to delete file.
     * 
     * @param path A file path to delete.
     */
    public static final void deleteFile(String path) {
        if (path != null) {
            deleteFile(project().getRoot().file(path));
        }
    }

    /**
     * Utility method to delete file.
     * 
     * @param file A file to delete.
     */
    public static final void deleteFile(File file) {
        if (file != null && file.isPresent()) {
            file.delete();
            ui().info("Delete file [", file.absolutize(), "]");
        }
    }

    /**
     * Utility method to delete directory.
     * 
     * @param path A directory path to delete.
     */
    public static final void deleteDirectory(String path) {
        if (path != null) {
            deletedirectory(project().getRoot().directory(path));
        }
    }

    /**
     * Utility method to delete directory.
     * 
     * @param dir A directory to delete.
     */
    public static final void deletedirectory(Directory dir) {
        if (dir != null && dir.isPresent()) {
            dir.delete();
            ui().info("Delete directory [", dir.absolutize(), "]");
        }
    }

    /**
     * Utility method to delete file.
     * 
     * @param from A file to copy.
     * @param to A destination.
     */
    public static final void copyFile(File from, File to) {
        if (from == null) {
            throw new Fail("The specified file is null.");
        }

        if (from.isAbsent()) {
            throw new Fail("File [" + from + "] is not found.");
        }

        from.copyTo(to);
        ui().info("Copy file from [", from.absolutize(), "] to [", to.absolutize() + "]");
    }

    /**
     * Utilitu method to unpack archive.
     * 
     * @param from
     * @param to
     */
    public static final void pack(Directory from, File to) {
        pack(from, to, UnaryOperator.identity());
    }

    /**
     * Utilitu method to unpack archive.
     * 
     * @param from
     * @param to
     */
    public static final void pack(Directory from, File to, UnaryOperator<Option> options) {
        if (from == null) {
            throw new Fail("The specified file is null.");
        }

        if (from.isAbsent()) {
            throw new Fail("File [" + from + "] is not found.");
        }

        from.trackPackingTo(to, options).to(progress -> {
            ui().trace("Packing ", from.name(), " to ", to, " (", progress.rateByFiles(), "%)");
        }, e -> {
            ui().error(e);
        }, () -> {
            ui().info("Packed ", from.name(), " to ", to);
        });
    }

    /**
     * Utilitu method to unpack archive.
     * 
     * @param from
     * @param to
     */
    public static final void unpack(File from, Directory to) {
        unpack(from, to, UnaryOperator.identity());
    }

    /**
     * Utilitu method to unpack archive.
     * 
     * @param from
     * @param to
     */
    public static final void unpack(File from, Directory to, UnaryOperator<Option> options) {
        if (from == null) {
            throw new Fail("The specified file is null.");
        }

        if (from.isAbsent()) {
            throw new Fail("File [" + from + "] is not found.");
        }

        from.trackUnpackingTo(to, options).to(progress -> {
            ui().trace("Unpacking ", from.name(), " to ", to, " (", progress.rateByFiles(), "%)");
        }, e -> {
            ui().error(e);
        }, () -> {
            ui().info("Unpacked ", from.name(), " to ", to);
        });
    }

    /**
     * Utility method to check file.
     * 
     * @param path A file path to check.
     */
    public static final boolean checkFile(String path) {
        if (path == null) {
            return false;
        }
        return checkFile(project().getRoot().file(path));
    }

    /**
     * Utility method to check file.
     * 
     * @param file A file to check.
     */
    public static final boolean checkFile(File file) {
        return file != null && file.isPresent();
    }

    /**
     * Execute required tasks.
     * 
     * @param task A task to execute.
     */
    public static final <T extends Task, R> R require(ValuedTaskReference<T, R> task) {
        return (R) requireParallel(new TaskReference[] {task});
    }

    /**
     * Execute required tasks.
     * 
     * @param task A task to execute.
     */
    public static final <T extends Task> void require(TaskReference<T> task) {
        requireParallel(new TaskReference[] {task});
    }

    /**
     * Execute required tasks in parallel.
     * 
     * @param task1 A task to execute.
     * @param task2 A task to execute.
     */
    public static final <T1 extends Task, T2 extends Task> void require(TaskReference<T1> task1, TaskReference<T2> task2) {
        requireParallel(new TaskReference[] {task1, task2});
    }

    /**
     * Execute required tasks in parallel.
     * 
     * @param task1 A task to execute.
     * @param task2 A task to execute.
     * @param task3 A task to execute.
     */
    public static final <T1 extends Task, T2 extends Task, T3 extends Task> void require(TaskReference<T1> task1, TaskReference<T2> task2, TaskReference<T3> task3) {
        requireParallel(new TaskReference[] {task1, task2, task3});
    }

    /**
     * Execute required tasks in parallel.
     * 
     * @param task1 A task to execute.
     * @param task2 A task to execute.
     * @param task3 A task to execute.
     * @param task4 A task to execute.
     */
    public static final <T1 extends Task, T2 extends Task, T3 extends Task, T4 extends Task> void require(TaskReference<T1> task1, TaskReference<T2> task2, TaskReference<T3> task3, TaskReference<T4> task4) {
        requireParallel(new TaskReference[] {task1, task2, task3, task4});
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
    public static final <T1 extends Task, T2 extends Task, T3 extends Task, T4 extends Task, T5 extends Task> void require(TaskReference<T1> task1, TaskReference<T2> task2, TaskReference<T3> task3, TaskReference<T4> task4, TaskReference<T5> task5) {
        requireParallel(new TaskReference[] {task1, task2, task3, task4, task5});
    }

    /**
     * Use other tasks.
     * 
     * @param tasks
     */
    private static Object requireParallel(TaskReference<Task>[] tasks) {
        LinkedList<ParallelInterface> parallels = new LinkedList();
        ParallelInterface parallel = null;

        for (int i = 0; i < tasks.length; i++) {
            parallels.addFirst(parallel = new ParallelInterface(ui(), parallel));
        }
        parallels.peekFirst().start();

        // cache the current project
        Project project = project();

        AtomicInteger index = new AtomicInteger();
        return I.signal(tasks).joinAllOrNone(task -> {
            String name = TaskInfo.computeTaskName(task);
            ParallelInterface ui = parallels.get(index.getAndIncrement());

            ForProject.local.set(project);
            ForUI.local.set(ui);
            String main = TaskInfo.current.get();
            try {
                System.out.println("stop " + main);
                return Bee.execute(name);
            } catch (Throwable e) {
                parallels.forEach(ParallelInterface::stop);

                throw new Fail("[" + TaskInfo.current.get() + "] invoked sub tasks " + Stream.of(tasks)
                        .map(TaskInfo::computeTaskName)
                        .toList() + " in parallel. But the task [" + name + "] has failed, so Bee aborts all other tasks.").reason(e);
            } finally {
                ui.finish();
                System.out.println("resume " + main);
            }
        }).waitForTerminate().to().v;
    }

    private static class ParallelInterface extends UserInterface {

        /** The message mode. */
        // buffering(0) → buffered(2)
        // ↓
        // processing(1)
        //
        // aborted(3)
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
        protected InputStream getSink() {
            return ui.getSink();
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
        protected synchronized void write(Throwable error) {
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
        protected synchronized void startCommand(String name) {
            switch (mode) {
            case 0:
                messages.add(() -> ui.startCommand(name));
                break;

            case 1:
                ui.startCommand(name);
                break;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected synchronized void endCommand(String name) {
            switch (mode) {
            case 0:
                messages.add(() -> ui.endCommand(name));
                break;

            case 1:
                ui.endCommand(name);
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

        /**
         * Stop this user interface forcely.
         */
        private void stop() {
            mode = 3;
            messages.clear();
            messages = null;
        }
    }
}