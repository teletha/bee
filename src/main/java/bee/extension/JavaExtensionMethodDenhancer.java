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

import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.ClassWriter;
import net.bytebuddy.jar.asm.Opcodes;

/**
 * @version 2016/12/15 23:24:51
 */
public class JavaExtensionMethodDenhancer extends ClassVisitor {

    /**
     * @param arg0
     */
    private JavaExtensionMethodDenhancer(ClassWriter writer) {
        super(Opcodes.ASM5, writer);
    }
}
