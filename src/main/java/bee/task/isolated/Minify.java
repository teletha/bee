/*
 * Copyright (C) 2025 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task.isolated;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;

import bee.task.Jar.Config;

/**
 * ASM MethodVisitor implementation to remove debug and trace information.
 */
public class Minify extends MethodVisitor {

    private final Config conf;

    /**
     * Creates a method visitor for minification.
     * 
     * @param methodVisitor The next visitor in the chain.
     * @param conf Configuration for debug/trace info removal.
     */
    public Minify(MethodVisitor methodVisitor, Config conf) {
        super(Opcodes.ASM9, methodVisitor);
        this.conf = conf;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitParameter(String name, int access) {
        if (!conf.removeDebugInfo) {
            super.visitParameter(name, access);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
        if (!conf.removeDebugInfo) {
            super.visitLocalVariable(name, descriptor, signature, start, end, index);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String descriptor, boolean visible) {
        if (!conf.removeDebugInfo) {
            return super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, descriptor, visible);
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitLineNumber(int line, Label start) {
        if (!conf.removeTraceInfo) {
            super.visitLineNumber(line, start);
        }
    }
}