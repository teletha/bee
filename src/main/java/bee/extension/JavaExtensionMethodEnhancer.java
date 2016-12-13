/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.extension;

import static jdk.internal.org.objectweb.asm.Opcodes.*;

import java.util.Arrays;
import java.util.List;

import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;
import kiss.I;

/**
 * @version 2016/12/13 11:24:39
 */
class JavaExtensionMethodEnhancer extends ClassVisitor {

    /** The definitions. */
    private final List<JavaExtensionMethodDefinition> definitions;

    /**
     * Class file enhancer.
     * 
     * @param writer An actual writer.
     * @param definitions A list of extension definitions.
     */
    JavaExtensionMethodEnhancer(ClassWriter writer, List<JavaExtensionMethodDefinition> definitions) {
        super(Opcodes.ASM5, writer);

        this.definitions = definitions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitEnd() {
        // add extension methods
        for (JavaExtensionMethodDefinition definition : definitions) {
            defineMethod(definition);
        }
        super.visitEnd();
    }

    MethodVisitor mv;

    /**
     * @param definition
     */
    private void defineMethod(JavaExtensionMethodDefinition definition) {
        String className = Type.getInternalName(definition.extensionClass);
        Type callee = Type.getMethodType(Type.getMethodDescriptor(definition.method));
        Class[] params = definition.method.getParameterTypes();
        Type[] param = Type.getArgumentTypes(definition.method);
        Type caller = Type
                .getMethodType(callee.getReturnType(), Arrays.asList(param).subList(1, param.length).toArray(new Type[param.length - 1]));

        int localIndex = params.length;

        mv = visitMethod(ACC_PUBLIC, definition.method.getName(), caller.getDescriptor(), definition.signature(), null);
        mv.visitCode();
        Label l0 = new Label();
        Label l1 = new Label();
        Label l2 = new Label();
        mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");
        mv.visitLabel(l0);

        Variable[] variables = new Variable[param.length];
        variables[0] = new That();

        for (int i = 1; i < variables.length; i++) {
            variables[i] = new Param(params[i], i);
        }
        Variable parameters = declareLocalVariable(createArray(Object.class, variables));

        mv.visitIntInsn(BIPUSH, params.length);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object"); // new Object[params.length]
        mv.visitInsn(DUP);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(AASTORE); // first parameter must be 'this'
        for (int i = 1; i < params.length; i++) {
            mv.visitInsn(DUP); // copy array
            mv.visitIntInsn(BIPUSH, i); // array index
            mv.visitVarInsn(param[i].getOpcode(ILOAD), i); // load param
            wrap(params[i], mv);
            mv.visitInsn(AASTORE); // store param into array
        }
        mv.visitVarInsn(ASTORE, localIndex); // store params into local variable

        Label l3 = new Label();
        mv.visitLabel(l3);
        mv.visitIntInsn(BIPUSH, params.length);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/String"); // new String[params.length]
        for (int i = 0; i < params.length; i++) {
            mv.visitInsn(DUP); // copy array
            mv.visitIntInsn(BIPUSH, i); // array index
            mv.visitLdcInsn(params[i].getName()); // load param type
            mv.visitInsn(AASTORE); // store param into array
        }
        mv.visitVarInsn(ASTORE, localIndex + 1); // store paramTypes into local variable

        Label l4 = new Label();
        mv.visitLabel(l4);
        mv.visitVarInsn(ALOAD, localIndex);
        mv.visitInsn(ARRAYLENGTH); // calculate param length
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Class"); // new Class[params.length]
        mv.visitVarInsn(ASTORE, localIndex + 2); // store types into local variable

        // for loop to load parameter classes
        Label l5 = new Label();
        mv.visitLabel(l5);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, localIndex + 3); // int i = 0;
        Label l6 = new Label();
        mv.visitLabel(l6);
        Label l7 = new Label();
        mv.visitJumpInsn(GOTO, l7);
        Label l8 = new Label();
        mv.visitLabel(l8);
        mv.visitVarInsn(ALOAD, localIndex + 2); // paramTypes
        mv.visitVarInsn(ILOAD, localIndex + 3); // i
        mv.visitVarInsn(ALOAD, localIndex + 1); // paramNames
        mv.visitVarInsn(ILOAD, localIndex + 3); // i
        mv.visitInsn(AALOAD); // paramNames[i]
        mv.visitInsn(ICONST_1); // true
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/ClassLoader", "getSystemClassLoader", "()Ljava/lang/ClassLoader;", false);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", false);
        mv.visitInsn(AASTORE);
        Label l9 = new Label();
        mv.visitLabel(l9);
        mv.visitIincInsn(localIndex + 3, 1); // increment i
        mv.visitLabel(l7);
        mv.visitVarInsn(ILOAD, localIndex + 3);
        mv.visitVarInsn(ALOAD, localIndex + 2);
        mv.visitInsn(ARRAYLENGTH);
        mv.visitJumpInsn(IF_ICMPLT, l8);

        // load extension class
        Label l10 = new Label();
        mv.visitLabel(l10);
        mv.visitLdcInsn(definition.extensionClass.getName()); // class name
        mv.visitInsn(ICONST_1); // true
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/ClassLoader", "getSystemClassLoader", "()Ljava/lang/ClassLoader;", false);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", false);
        mv.visitVarInsn(ASTORE, localIndex + 3); // store extensionClass into local variable

        // load extension method
        Label l11 = new Label();
        mv.visitLabel(l11);
        mv.visitVarInsn(ALOAD, localIndex + 3); // load extension class
        mv.visitLdcInsn(definition.method.getName()); // load method name
        mv.visitVarInsn(ALOAD, localIndex + 2); // load parameter types
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", false);

        // invoke extension method
        mv.visitInsn(ACONST_NULL);
        mv.visitVarInsn(ALOAD, localIndex); // load actual parameters
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);

        // check return type
        cast(definition.method.getReturnType(), mv);

        // return
        mv.visitLabel(l1);
        mv.visitInsn(Type.getReturnType(definition.method).getOpcode(IRETURN));

        // catch exception
        mv.visitLabel(l2);
        mv.visitFrame(Opcodes.F_FULL, 1, new Object[] {className}, 1, new Object[] {"java/lang/Exception"});
        mv.visitVarInsn(ASTORE, 1);
        Label l12 = new Label();
        mv.visitLabel(l12);
        mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/Throwable;)V", false);
        mv.visitInsn(ATHROW);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * @param createArray
     * @return
     */
    private Variable declareLocalVariable(Array array) {
        mv.visitVarInsn(ASTORE, localIndex); // store params into local variable
        return new Variable();
    }

    /**
     * @param class1
     * @param variables
     * @return
     */
    private <T> Array<T> createArray(Class<T> class1, Variable[] variables) {
        return null;
    }

    /**
     * Helper method to write cast code. This cast mostly means down cast. (e.g. Object -> String,
     * Object -> int)
     *
     * @param clazz A class to cast.
     * @return A class type to be casted.
     */
    private Type cast(Class clazz, MethodVisitor mv) {
        Type type = Type.getType(clazz);

        if (clazz.isPrimitive()) {
            if (clazz != Void.TYPE) {
                Type wrapper = Type.getType(I.wrap(clazz));
                mv.visitTypeInsn(CHECKCAST, wrapper.getInternalName());
                mv.visitMethodInsn(INVOKEVIRTUAL, wrapper.getInternalName(), clazz.getName() + "Value", "()" + type.getDescriptor(), false);
            }
        } else {
            mv.visitTypeInsn(CHECKCAST, type.getInternalName());
        }

        // API definition
        return type;
    }

    /**
     * Helper method to write cast code. This cast mostly means up cast. (e.g. String -> Object, int
     * -> Integer)
     *
     * @param clazz A primitive class type to wrap.
     */
    private void wrap(Class clazz, MethodVisitor mv) {
        if (clazz.isPrimitive() && clazz != Void.TYPE) {
            Type wrapper = Type.getType(I.wrap(clazz));
            mv.visitMethodInsn(INVOKESTATIC, wrapper
                    .getInternalName(), "valueOf", "(" + Type.getType(clazz).getDescriptor() + ")" + wrapper.getDescriptor(), false);
        }
    }

    /**
     * @version 2016/12/14 1:32:07
     */
    private class Writer {

    }

    /**
     * @version 2016/12/14 0:21:26
     */
    private class Variable {

        private Class type;

        private int index;

        /**
         * @param type
         * @param index
         */
        private Variable(Class type, int index) {
            this.type = type;
            this.index = index;
        }
    }

    /**
     * @version 2016/12/14 1:33:42
     */
    private class That extends Variable {

        /**
         * 
         */
        private That() {
            super(Object.class, 0);
        }
    }

    /**
     * @version 2016/12/14 1:33:40
     */
    private class Param extends Variable {

        /**
         * @param type
         * @param index
         */
        public Param(Class type, int index) {
            super(type, index);
        }
    }

    /**
     * @version 2016/12/14 1:35:14
     */
    private class Array<T> {

    }
}