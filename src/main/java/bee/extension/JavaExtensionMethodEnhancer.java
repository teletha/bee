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
import java.util.function.Consumer;

import bee.extension.JavaExtensionMethodEnhancer.Writer.Instance;
import bee.extension.JavaExtensionMethodEnhancer.Writer.Variable;
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
        for (JavaExtensionMethodDefinition def : definitions) {
            java.lang.reflect.Method m = def.method;
            Class[] parameters = m.getParameterTypes();
            Class[] tail = Arrays.copyOfRange(parameters, 1, parameters.length);

            Writer.defineMethod(this, ACC_PUBLIC, m.getName(), m.getReturnType(), tail, m.getExceptionTypes(), def.signature(), w -> {
                Label l0 = new Label();
                Label l1 = new Label();
                Label l2 = new Label();
                w.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");
                w.visitLabel(l0);

                Instance[] variables = new Instance[parameters.length];
                variables[0] = w.self;
                for (int i = 1; i < parameters.length; i++) {
                    variables[i] = w.param(i - 1);
                }
                Variable params = w.declareLocalVariable(w.createArray(Object.class, variables));

                Instance[] classNames = new Instance[parameters.length];
                for (int i = 0; i < parameters.length; i++) {
                    classNames[i] = w.string(parameters[i].getName());
                }
                Variable paramTypeNames = w.declareLocalVariable(w.createArray(String.class, classNames));
                Variable paramTypes = w.declareLocalVariable(w.createArray(Class.class, parameters.length));

                // for loop to load parameter classes
                Label l5 = new Label();
                w.visitLabel(l5);
                w.visitInsn(ICONST_0);
                w.visitVarInsn(ISTORE, w.localIndex + 3); // int i = 0;
                Label l6 = new Label();
                w.visitLabel(l6);
                Label l7 = new Label();
                w.visitJumpInsn(GOTO, l7);
                Label l8 = new Label();
                w.visitLabel(l8);
                w.visitVarInsn(ALOAD, w.localIndex + 2); // paramTypes
                w.visitVarInsn(ILOAD, w.localIndex + 3); // i
                w.visitVarInsn(ALOAD, w.localIndex + 1); // paramNames
                w.visitVarInsn(ILOAD, w.localIndex + 3); // i
                w.visitInsn(AALOAD); // paramNames[i]
                w.visitInsn(ICONST_1); // true
                w.visitMethodInsn(INVOKESTATIC, "java/lang/ClassLoader", "getSystemClassLoader", "()Ljava/lang/ClassLoader;", false);
                w.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", false);
                w.visitInsn(AASTORE);
                Label l9 = new Label();
                w.visitLabel(l9);
                w.visitIincInsn(w.localIndex + 3, 1); // increment i
                w.visitLabel(l7);
                w.visitVarInsn(ILOAD, w.localIndex + 3);
                w.visitVarInsn(ALOAD, w.localIndex + 2);
                w.visitInsn(ARRAYLENGTH);
                w.visitJumpInsn(IF_ICMPLT, l8);

                // load extension class
                Label l10 = new Label();
                w.visitLabel(l10);
                w.visitLdcInsn(def.extensionClass.getName()); // class name
                w.visitInsn(ICONST_1); // true
                w.visitMethodInsn(INVOKESTATIC, "java/lang/ClassLoader", "getSystemClassLoader", "()Ljava/lang/ClassLoader;", false);
                w.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", false);
                w.visitVarInsn(ASTORE, w.localIndex + 3); // store extensionClass into
                                                          // local
                                                          // variable

                // load extension method
                Label l11 = new Label();
                w.visitLabel(l11);
                w.visitVarInsn(ALOAD, w.localIndex + 3); // load extension class
                w.visitLdcInsn(w.methodName); // load method name
                w.visitVarInsn(ALOAD, w.localIndex + 2); // load parameter types
                w.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", false);

                // invoke extension method
                w.visitInsn(ACONST_NULL);
                w.visitVarInsn(ALOAD, w.localIndex); // load actual parameters
                w.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);

                // check return type
                w.cast(w.returnClass);

                // return
                w.visitLabel(l1);
                w.visitInsn(w.returnType.getOpcode(IRETURN));

                // catch exception
                w.visitLabel(l2);
                w.visitFrame(Opcodes.F_FULL, 1, new Object[] {Type.getInternalName(def.extensionClass)}, 1, new Object[] {
                        "java/lang/Exception"});
                w.visitVarInsn(ASTORE, 1);
                Label l12 = new Label();
                w.visitLabel(l12);
                w.visitTypeInsn(NEW, "java/lang/RuntimeException");
                w.visitInsn(DUP);
                w.visitVarInsn(ALOAD, 1);
                w.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/Throwable;)V", false);
                w.visitInsn(ATHROW);
                w.visitMaxs(0, 0);
            });
        }
        super.visitEnd();
    }

    /**
     * @version 2016/12/14 9:32:26
     */
    public static class Writer extends MethodVisitor {

        protected final String methodName;

        protected final Class[] params;

        protected final Class returnClass;

        protected final Type returnType;

        protected final int localIndex;

        private int local;

        public final Variable self;

        // /**
        // * @param createArray
        // * @return
        // */
        // private Variable declareLocalVariable(Array array) {
        // w.visitVarInsn(ASTORE, localIndex); // store params into local variable
        // return new Variable();
        // }

        /**
         * @param cv
         * @param modifier
         * @param name
         * @param returnClass
         * @param paramClass
         * @param throwClass
         * @param signature
         * @param writer
         */
        private Writer(ClassVisitor cv, int modifier, String name, Class returnClass, Class[] paramClass, Class[] throwClass, String signature, Consumer<Writer> writer) {
            super(Opcodes.ASM5, cv.visitMethod(modifier, name, Type.getMethodType(Type.getType(returnClass), convert(paramClass))
                    .getDescriptor(), signature, null));

            this.methodName = name;
            this.params = paramClass;
            this.localIndex = params.length + 1;
            this.returnClass = returnClass;
            this.returnType = Type.getType(returnClass);
            this.self = new Self();
            this.local = localIndex;

            mv.visitCode();
            writer.accept(this);
            mv.visitEnd();
        }

        /**
         * <p>
         * Define new method.
         * </p>
         * 
         * @param cv
         * @param modifier
         * @param name
         * @param returnClass
         * @param paramClass
         * @param throwClass
         * @param signature
         * @return
         */
        public static void defineMethod(ClassVisitor cv, int modifier, String name, Class returnClass, Class[] paramClass, Class[] throwClass, String signature, Consumer<Writer> writer) {
            new Writer(cv, modifier, name, returnClass, paramClass, throwClass, signature, writer);
        }

        /**
         * @param type
         * @param items
         * @return
         */
        public <T> Array<T> createArray(Class<T> type, Instance[] items) {
            return new Array(type, items);
        }

        /**
         * @param type
         * @param items
         * @return
         */
        public <T> Array<T> createArray(Class<T> type, int size) {
            return new Array(type, size);
        }

        // /**
        // * @param createArray
        // * @return
        // */
        // private Variable declareLocalVariable(Array array) {
        // w.visitVarInsn(ASTORE, localIndex); // store params into local variable
        // return new Variable();
        // }

        /**
         * @param createArray
         * @return
         */
        public Variable declareLocalVariable(Array array) {
            Variable variable = new Variable(array.type, nextLocalId());
            variable.set(array);

            return variable;
        }

        private int nextLocalId() {
            return local++;
        }

        /**
         * <p>
         * Convert {@link Class} to {@link Type}.
         * </p>
         * 
         * @param items
         * @return
         */
        private static Type[] convert(Class[] items) {
            Type[] types = new Type[items.length];

            for (int i = 0; i < types.length; i++) {
                types[i] = Type.getType(items[i]);
            }
            return types;
        }

        /**
         * Helper method to write cast code. This cast mostly means down cast. (e.g. Object ->
         * String, Object -> int)
         *
         * @param clazz A class to cast.
         * @return A class type to be casted.
         */
        protected Type cast(Class clazz) {
            Type type = Type.getType(clazz);

            if (clazz.isPrimitive()) {
                if (clazz != Void.TYPE) {
                    Type wrapper = Type.getType(I.wrap(clazz));
                    mv.visitTypeInsn(CHECKCAST, wrapper.getInternalName());
                    mv.visitMethodInsn(INVOKEVIRTUAL, wrapper
                            .getInternalName(), clazz.getName() + "Value", "()" + type.getDescriptor(), false);
                }
            } else {
                mv.visitTypeInsn(CHECKCAST, type.getInternalName());
            }

            // API definition
            return type;
        }

        /**
         * Helper method to write cast code. This cast mostly means up cast. (e.g. String -> Object,
         * int -> Integer)
         *
         * @param clazz A primitive class type to wrap.
         */
        protected void wrap(Class clazz) {
            if (clazz.isPrimitive() && clazz != Void.TYPE) {
                Type wrapper = Type.getType(I.wrap(clazz));
                mv.visitMethodInsn(INVOKESTATIC, wrapper
                        .getInternalName(), "valueOf", "(" + Type.getType(clazz).getDescriptor() + ")" + wrapper.getDescriptor(), false);
            }
        }

        /**
         * <p>
         * Create parameter access.
         * </p>
         * 
         * @param index
         * @return
         */
        public Variable param(int index) {
            if (index < 0) {
                throw new IllegalArgumentException("Negative index is invalid in method parameters.");
            }

            if (params.length <= index) {
                throw new IllegalArgumentException("Out of index of " + methodName + " parameters. :" + index);
            }
            return new Param(params[index], index + 1);
        }

        public Instance string(String value) {
            return new StringExpression(value);
        }

        /**
         * @version 2016/12/14 0:21:26
         */
        public class Variable extends Instance {

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

            /**
             * <p>
             * Load variable.
             * </p>
             */
            public void get() {
                visitVarInsn(Type.getType(type).getOpcode(ILOAD), index);
                wrap(type);
            }

            /**
             * @param array
             */
            public void set(Instance instance) {
                instance.write();
                visitVarInsn(Type.getType(type).getOpcode(ISTORE), index);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void write() {
                get();
            }
        }

        /**
         * @version 2016/12/14 1:33:42
         */
        private class Self extends Variable {

            /**
             * 
             */
            private Self() {
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
         * @version 2016/12/14 14:28:02
         */
        public class Instance {

            public void write() {
            }
        }

        /**
         * @version 2016/12/14 14:34:23
         */
        public class StringExpression extends Instance {

            private final String value;

            /**
             * @param value
             */
            private StringExpression(String value) {
                this.value = value;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void write() {
                visitLdcInsn(value);
            }
        }

        /**
         * @version 2016/12/14 1:35:14
         */
        private class Array<T> extends Instance {

            private Class type;

            private Instance[] items;

            /**
             * @param type
             * @param items
             */
            public Array(Class type, Instance[] items) {
                this.type = type;
                this.items = items;
            }

            /**
             * @param type
             * @param items
             */
            public Array(Class type, int size) {
                this.type = type;
                this.items = new Instance[size];
            }

            /**
             * <p>
             * Initialize array.
             * </p>
             */
            @Override
            public void write() {
                // new Type[items.length]
                visitIntInsn(BIPUSH, items.length);
                visitTypeInsn(ANEWARRAY, Type.getType(type).getInternalName());

                for (int i = 0; i < items.length; i++) {
                    visitInsn(DUP);
                    visitIntInsn(BIPUSH, i); // array index
                    if (items[i] != null) items[i].write();
                    visitInsn(AASTORE); // store param into array
                }
            }
        }
    }
}