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

import static org.objectweb.asm.Type.*;

import org.objectweb.asm.MethodVisitor;

/**
 * @version 2012/01/18 9:56:27
 */
public class Constant extends Bytecode<Constant> {

    /** The constant value. */
    private Object value;

    /**
     * @param value
     */
    public Constant(Object value) {
        this.value = value;
    }

    /**
     * @see testament.bytecode.Bytecode#write(org.objectweb.asm.MethodVisitor, boolean)
     */
    @Override
    public void write(MethodVisitor visitor, boolean requireNonPrimitive) {
        visitor.visitLdcInsn(value);

        if (value instanceof Integer) {
            wrap(visitor, INT_TYPE);
        } else if (value instanceof Long) {
            wrap(visitor, LONG_TYPE);
        } else if (value instanceof Float) {
            wrap(visitor, FLOAT_TYPE);
        } else if (value instanceof Double) {
            wrap(visitor, DOUBLE_TYPE);
        }
    }
}
