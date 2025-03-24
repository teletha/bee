/*
 * Copyright (C) 2024 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee;

import static bee.TaskOperations.*;
import static org.objectweb.asm.Opcodes.*;

import java.io.InputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import java.util.function.Function;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import bee.Task.TaskLifestyle;
import bee.api.Command;
import bee.api.Project;
import bee.util.EnhancedClassWriter;
import bee.util.EnhancedMethodWriter;
import bee.util.Inputs;
import kiss.Extensible;
import kiss.I;
import kiss.Lifestyle;
import kiss.Managed;
import kiss.Model;

@Managed(value = TaskLifestyle.class)
public abstract class Task implements Extensible {

    /**
     * Execute manual tasks.
     */
    public void execute() {
        // do nothing
    }

    @Command("Display help message for all commands of this task.")
    public void help() {
        TaskInfo info = TaskInfo.by(TaskInfo.computeTaskName(getClass()));

        for (Entry<String, String> entry : info.descriptions.entrySet()) {
            // display usage description for this command
            ui().info(entry.getKey(), " - ", entry.getValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Model.of(this).type.getSimpleName();
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
            parallels.offerFirst(parallel = new ParallelInterface(ui(), parallel));
        }
        parallels.peekFirst().start();

        // cache the current project
        Project project = project();

        return I.signal(tasks).joinAll(task -> {
            LifestyleForProject.local.set(project);
            LifestyleForUI.local.set(parallels.pollFirst());

            Method m = task.getClass().getDeclaredMethod("writeReplace");
            m.setAccessible(true);

            SerializedLambda s = (SerializedLambda) m.invoke(task);
            Method method = I.type(s.getImplClass().replaceAll("/", ".")).getMethod(s.getImplMethodName());

            return Bee.execute(TaskInfo.computeTaskName(method.getDeclaringClass()) + ":" + method.getName().toLowerCase());
        }).to().v;
    }

    /**
     * 
     */
    static class ParallelInterface extends UserInterface {

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
        synchronized void finish() {
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
     * Store for each command result.
     */
    @SuppressWarnings("serial")
    private static class Cache extends HashMap<String, Object> {
    }

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
                String parent = Type.getInternalName(model);
                writer.visit(V21, ACC_PUBLIC | ACC_SUPER | ACC_SYNTHETIC, writer.classInternalName, null, parent, null);

                // constructor
                EnhancedMethodWriter mw = writer.writeMethod(ACC_PUBLIC, "<init>", "()V", null, null);
                mw.visitVarInsn(ALOAD, 0);
                mw.visitMethodInsn(INVOKESPECIAL, parent, "<init>", "()V", false);
                mw.visitInsn(RETURN);
                mw.visitMaxs(1, 1);
                mw.visitEnd();

                // overwrite command methods
                Map<Method, List<Annotation>> methods = Model.collectAnnotatedMethods(model);
                for (Entry<Method, List<Annotation>> entry : methods.entrySet()) {
                    if (entry.getValue().stream().anyMatch(x -> x.annotationType() == Command.class)) {
                        Method m = entry.getKey();
                        String methodName = m.getName();
                        String methodDesc = Type.getMethodDescriptor(m);
                        Type returnType = Type.getReturnType(m);
                        boolean valued = m.getReturnType() != void.class;

                        mw = writer.writeMethod(ACC_PUBLIC, methodName, methodDesc, null, null);
                        mw.visitLdcInsn(model.getSimpleName() + ":" + Inputs.hyphenize(methodName));
                        mw.visitMethodInsn(INVOKESTATIC, "bee/util/Inputs", "hyphenize", "(Ljava/lang/String;)Ljava/lang/String;", false);
                        mw.visitVarInsn(ASTORE, 1);

                        mw.visitMethodInsn(INVOKESTATIC, "bee/TaskOperations", "project", "()Lbee/api/Project;", false);
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
                        mw.visitMethodInsn(INVOKESTATIC, "bee/TaskOperations", "ui", "()Lbee/UserInterface;", false);

                        mw.visitVarInsn(ALOAD, 1);
                        mw.visitInsn(ACONST_NULL);
                        mw.visitMethodInsn(INVOKEVIRTUAL, "bee/UserInterface", "startCommand", "(Ljava/lang/String;Lbee/api/Command;)V", false);
                        mw.visitVarInsn(ALOAD, 0);
                        mw.visitMethodInsn(INVOKESPECIAL, parent, methodName, methodDesc, false);
                        if (valued) {
                            mw.wrap(returnType);
                            mw.visitVarInsn(ASTORE, 3);
                        }

                        mw.visitMethodInsn(INVOKESTATIC, "bee/TaskOperations", "ui", "()Lbee/UserInterface;", false);
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