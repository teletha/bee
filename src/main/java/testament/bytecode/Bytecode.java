/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package testament.bytecode;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 * @version 2012/01/18 8:42:38
 */
public abstract class Bytecode<T extends Bytecode<T>> {

    /** The object type for reuse. */
    public static final Type OBJECT_TYPE = Type.getType(Object.class);

    /**
     * <p>
     * Helper method to write wrap primitive type.
     * </p>
     * 
     * @param type
     * @return
     */
    final void wrap(MethodVisitor visitor, Type type) {
        Type wrapper = getWrapperType(type);

        if (wrapper != type) {
            visitor.visitMethodInsn(INVOKESTATIC, wrapper.getInternalName(), "valueOf", Type.getMethodDescriptor(wrapper, type));
        }
    }

    /**
     * <p>
     * Write bytecode actually.
     * </p>
     * 
     * @param visitor
     * @param requireNonPrimitive TODO
     */
    public abstract void write(MethodVisitor visitor, boolean requireNonPrimitive);

    /**
     * <p>
     * Search wrapper type of the specified primitive type.
     * </p>
     * 
     * @param type
     * @return
     */
    public static Type getWrapperType(Type type) {
        switch (type.getSort()) {
        case Type.BOOLEAN:
            return Type.getType(Boolean.class);

        case Type.INT:
            return Type.getType(Integer.class);

        case Type.LONG:
            return Type.getType(Long.class);

        case Type.FLOAT:
            return Type.getType(Float.class);

        case Type.DOUBLE:
            return Type.getType(Double.class);

        case Type.CHAR:
            return Type.getType(Character.class);

        case Type.BYTE:
            return Type.getType(Byte.class);

        case Type.SHORT:
            return Type.getType(Short.class);

        default:
            return type;
        }
    }
}
