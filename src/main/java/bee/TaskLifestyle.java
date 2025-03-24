/*
 * Copyright (C) 2025 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee;

import static org.objectweb.asm.Opcodes.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import bee.api.Command;
import bee.util.EnhancedClassWriter;
import bee.util.EnhancedMethodWriter;
import bee.util.Inputs;
import kiss.I;
import kiss.Lifestyle;
import kiss.Model;

/**
 * 
 */
class TaskLifestyle implements Lifestyle<Object> {

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

    /**
     * Store for each command result.
     */
    @SuppressWarnings("serial")
    private static class Cache extends HashMap<String, Object> {
    }
}