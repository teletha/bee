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

                // Variable[] variables = new Variable[param.length];
                // variables[0] = new That();
                //
                // for (int i = 1; i < variables.length; i++) {
                // variables[i] = new Param(params[i], i);
                // }
                // Variable parameters = declareLocalVariable(createArray(Object.class,
                // variables));

                w.visitIntInsn(BIPUSH, w.params.length);
                w.visitTypeInsn(ANEWARRAY, "java/lang/Object"); // new
                                                                // Object[params.length]
                w.visitInsn(DUP);
                w.visitInsn(ICONST_0);
                w.visitVarInsn(ALOAD, 0);
                w.visitInsn(AASTORE); // first parameter must be 'this'
                for (int i = 1; i < w.params.length; i++) {
                    w.visitInsn(DUP); // copy array
                    w.visitIntInsn(BIPUSH, i); // array index
                    w.visitVarInsn(Type.getType(w.params[i]).getOpcode(ILOAD), i); // load
                                                                                   // param
                    w.wrap(w.params[i]);
                    w.visitInsn(AASTORE); // store param into array
                }
                w.visitVarInsn(ASTORE, w.localIndex); // store params into local variable

                Label l3 = new Label();
                w.visitLabel(l3);
                w.visitIntInsn(BIPUSH, w.params.length);
                w.visitTypeInsn(ANEWARRAY, "java/lang/String"); // new
                                                                // String[params.length]
                for (int i = 0; i < parameters.length; i++) {
                    w.visitInsn(DUP); // copy array
                    w.visitIntInsn(BIPUSH, i); // array index
                    w.visitLdcInsn(parameters[i].getName()); // load param type
                    w.visitInsn(AASTORE); // store param into array
                }
                w.visitVarInsn(ASTORE, w.localIndex + 1); // store paramTypes into local
                                                          // variable

                Label l4 = new Label();
                w.visitLabel(l4);
                w.visitVarInsn(ALOAD, w.localIndex + 1);
                w.visitInsn(ARRAYLENGTH); // calculate param length
                w.visitTypeInsn(ANEWARRAY, "java/lang/Class"); // new Class[params.length]
                w.visitVarInsn(ASTORE, w.localIndex + 2); // store types into local
                                                          // variable

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
    private static class Writer extends MethodVisitor {

        protected final String methodName;

        protected final Class[] params;

        protected final Class returnClass;

        protected final Type returnType;

        protected final int localIndex;

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
         * @param class1
         * @param variables
         * @return
         */
        public <T> Array<T> createArray(Class<T> class1, Variable[] variables) {
            return null;
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