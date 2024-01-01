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

import java.lang.invoke.MethodHandles;
import java.util.function.Consumer;

import org.objectweb.asm.ClassWriter;

import kiss.I;

public class EnhancedClassWriter extends ClassWriter {

    /** The simple class name. */
    public final String className;

    /** The fqcn. */
    public final String fqcnName;

    /** The internal class name. */
    public final String classInternalName;

    /**
     * @param packageBaseClass
     * @param className
     */
    private EnhancedClassWriter(String className, String fqcn) {
        super(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        this.className = className;
        this.fqcnName = fqcn;
        this.classInternalName = fqcnName.replace('.', '/');
    }

    /**
     * Define new method.
     * 
     * @param access
     * @param name
     * @param descriptor
     * @param signature
     * @param exceptions
     * @return
     */
    public final EnhancedMethodWriter writeMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
        return new EnhancedMethodWriter(visitMethod(access, name, descriptor, signature, exceptions));
    }

    /**
     * Define the generated class at the specified package of base class.
     * 
     * @return The new defined class.
     */
    public static synchronized Class define(Class packageBaseClass, String className, Consumer<EnhancedClassWriter> writer) {
        String fqcnName = packageBaseClass.getPackageName() + "." + className;

        try {
            return ClassLoader.getSystemClassLoader().loadClass(fqcnName);
        } catch (ClassNotFoundException e) {
            try {
                EnhancedClassWriter w = new EnhancedClassWriter(className, fqcnName);
                writer.accept(w);
                return MethodHandles.privateLookupIn(packageBaseClass, MethodHandles.lookup()).defineClass(w.toByteArray());
            } catch (IllegalAccessException x) {
                throw I.quiet(e);
            }
        }
    }
}