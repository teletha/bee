/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.util;

import java.lang.invoke.MethodHandles;

import org.objectweb.asm.ClassWriter;

import kiss.I;

public class EnhancedClassWriter extends ClassWriter {

    /** The package base class. */
    private Class packageBaseClass;

    /** The simple class name. */
    public final String className;

    public final String fqcnName;

    /** The internal class name. */
    public final String classInternalName;

    /**
     * @param packageBaseClass
     * @param className
     */
    public EnhancedClassWriter(Class packageBaseClass, String className) {
        super(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        this.packageBaseClass = packageBaseClass;
        this.className = className;
        this.fqcnName = packageBaseClass.getPackageName() + "." + className;
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
    public final Class define() {
        try {
            MethodHandles.privateLookupIn(packageBaseClass, MethodHandles.lookup()).defineClass(toByteArray());

            return I.type(fqcnName);
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }
}
