/*
 * Copyright (C) 2018 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.extension;

import static net.bytebuddy.jar.asm.Opcodes.*;

import java.util.List;

import kiss.I;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.ClassWriter;
import net.bytebuddy.jar.asm.Label;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.jar.asm.Type;

/**
 * @version 2016/12/14 15:55:59
 */
class JavaExtensionMethodEnhancer extends ClassVisitor {

    /** The target class internal name to extend. */
    private final String extensionClassName;

    /** The definitions. */
    private final List<JavaExtensionMethodDefinition> definitions;

    /**
     * Class file enhancer.
     * 
     * @param writer An actual writer.
     * @param definitions A list of extension definitions.
     */
    JavaExtensionMethodEnhancer(ClassWriter writer, Class target, List<JavaExtensionMethodDefinition> definitions) {
        super(Opcodes.ASM5, writer);

        this.extensionClassName = Type.getInternalName(target);
        this.definitions = definitions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitEnd() {
        writeMethodInvoker();
        writeClassLoader();

        // add extension methods
        for (JavaExtensionMethodDefinition def : definitions) {
            String name = def.method.getName();
            Class returnClass = def.method.getReturnType();
            Type returnType = Type.getType(returnClass);
            Class[] parameters = def.method.getParameterTypes();
            Type[] tails = new Type[parameters.length - 1];
            for (int i = 1; i < parameters.length; i++) {
                tails[i - 1] = Type.getType(parameters[i]);
            }

            MethodVisitor mv = visitMethod(ACC_PUBLIC, name, Type.getMethodType(returnType, tails).getDescriptor(), def.signature(), null);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitLdcInsn(def.method.getDeclaringClass().getName());
            mv.visitLdcInsn(name);

            // new String[] {"ExtensionClassName", "param2ClassName", "param3ClassName", ...}
            mv.visitIntInsn(BIPUSH, parameters.length);
            mv.visitTypeInsn(ANEWARRAY, "java/lang/String");
            for (int i = 0; i < parameters.length; i++) {
                mv.visitInsn(DUP);
                mv.visitIntInsn(BIPUSH, i);
                mv.visitLdcInsn(parameters[i].getName());
                mv.visitInsn(AASTORE);
            }

            // new Object[] {this, param2, param3, ...}
            mv.visitIntInsn(BIPUSH, parameters.length);
            mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
            mv.visitInsn(DUP);
            mv.visitInsn(ICONST_0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(AASTORE); // first parameter must be 'this'
            for (int i = 1; i < parameters.length; i++) {
                mv.visitInsn(DUP);
                mv.visitIntInsn(BIPUSH, i);
                mv.visitVarInsn(Type.getType(parameters[i]).getOpcode(ILOAD), i);
                wrap(parameters[i], mv);
                mv.visitInsn(AASTORE);
            }

            // invoke method
            mv.visitMethodInsn(INVOKESPECIAL, extensionClassName, "$call$", "(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;", false);

            // check return type
            cast(returnClass, mv);

            // return
            mv.visitInsn(returnType.getOpcode(IRETURN));

            // clean up
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        super.visitEnd();
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
     * <p>
     * Write helper method code. (COPY from ASMfier)
     * </p>
     * <pre>
        private Object $call$(String extensionClassName, String extensionMethodName, String[] paramNames, Object[] params) {
            try {
                Class[] paramTypes = new Class[paramNames.length];
        
                for (int i = 0; i < paramTypes.length; i++) {
                    paramTypes[i] = $class$(paramNames[i]);
                }
                return $class$(extensionClassName).getMethod(extensionMethodName, paramTypes).invoke(null, params);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
     * </pre>
     */
    private void writeMethodInvoker() {
        MethodVisitor mv = visitMethod(ACC_PRIVATE, "$call$", "(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;", null, null);
        Label l0 = new Label();
        Label l1 = new Label();
        Label l2 = new Label();
        mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");
        mv.visitLabel(l0);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitInsn(ARRAYLENGTH);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
        mv.visitVarInsn(ASTORE, 5);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 6);
        Label l5 = new Label();
        mv.visitJumpInsn(GOTO, l5);
        Label l6 = new Label();
        mv.visitLabel(l6);
        mv.visitFrame(Opcodes.F_APPEND, 2, new Object[] {"[Ljava/lang/Class;", Opcodes.INTEGER}, 0, null);
        mv.visitVarInsn(ALOAD, 5);
        mv.visitVarInsn(ILOAD, 6);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitVarInsn(ILOAD, 6);
        mv.visitInsn(AALOAD);
        mv.visitMethodInsn(INVOKESPECIAL, extensionClassName, "$class$", "(Ljava/lang/String;)Ljava/lang/Class;", false);
        mv.visitInsn(AASTORE);
        mv.visitIincInsn(6, 1);
        mv.visitLabel(l5);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitVarInsn(ILOAD, 6);
        mv.visitVarInsn(ALOAD, 5);
        mv.visitInsn(ARRAYLENGTH);
        mv.visitJumpInsn(IF_ICMPLT, l6);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESPECIAL, extensionClassName, "$class$", "(Ljava/lang/String;)Ljava/lang/Class;", false);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitVarInsn(ALOAD, 5);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", false);
        mv.visitInsn(ACONST_NULL);
        mv.visitVarInsn(ALOAD, 4);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);
        mv.visitLabel(l1);
        mv.visitInsn(ARETURN);
        mv.visitLabel(l2);
        mv.visitLineNumber(31, l2);
        mv.visitFrame(Opcodes.F_FULL, 5, new Object[] {extensionClassName, "java/lang/String", "java/lang/String", "[Ljava/lang/String;",
                "[Ljava/lang/Object;"}, 1, new Object[] {"java/lang/Exception"});
        mv.visitVarInsn(ASTORE, 5);
        mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 5);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/Throwable;)V", false);
        mv.visitInsn(ATHROW);
        mv.visitMaxs(5, 7);
        mv.visitEnd();
    }

    /**
     * <p>
     * Write helper method code. (COPY from ASMfier)
     * </p>
     * <pre>
        private Class $class$(String name) throws ClassNotFoundException {
            return Class.forName(name, true, ClassLoader.getSystemClassLoader());
        }
     * </pre>
     */
    private void writeClassLoader() {
        MethodVisitor mv = visitMethod(ACC_PRIVATE, "$class$", "(Ljava/lang/String;)Ljava/lang/Class;", null, new String[] {
                "java/lang/ClassNotFoundException"});
        mv.visitVarInsn(ALOAD, 1);
        mv.visitInsn(ICONST_1);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/ClassLoader", "getSystemClassLoader", "()Ljava/lang/ClassLoader;", false);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(3, 2);
        mv.visitEnd();
    }
}
