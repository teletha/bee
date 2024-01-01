/*
 * Copyright (C) 2024 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.util;

import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class EnhancedMethodWriter extends MethodVisitor {

    /**
     * @param methodVisitor
     */
    public EnhancedMethodWriter(MethodVisitor methodVisitor) {
        super(ASM9, methodVisitor);
    }

    /**
     * Helper method to write bytecode which wrap from the primitive value.
     * 
     * @param type
     */
    public final void wrap(Type type) {
        Type wrapper = getWrapperType(type);

        if (wrapper != type) {
            mv.visitMethodInsn(INVOKESTATIC, wrapper.getInternalName(), "valueOf", Type.getMethodDescriptor(wrapper, type), false);
        }
    }

    /**
     * Helper method to write bytecode which unwrap to the primitive value.
     * 
     * @param type
     */
    public final void unwrap(Type type) {
        Type wrapper = getWrapperType(type);

        mv.visitTypeInsn(CHECKCAST, wrapper.getInternalName());
        if (wrapper != type) {
            mv.visitMethodInsn(INVOKEVIRTUAL, wrapper
                    .getInternalName(), type.getClassName() + "Value", "()" + type.getInternalName(), false);
        }
    }

    /**
     * Search wrapper type of the specified primitive type.
     * 
     * @param type
     * @return
     */
    private Type getWrapperType(Type type) {
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