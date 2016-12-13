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

import java.lang.reflect.Method;
import java.nio.file.Path;

import bee.Platform;
import kiss.I;

/**
 * @version 2016/12/13 10:30:54
 */
class JavaExtensionMethodDefinition {

    /** The target class to extend. */
    final Class target;

    /** The archive of the target class. */
    final Path archive;

    /** The extension class. */
    final Class extensionClass;

    /** The extension method. */
    final Method method;

    /**
     * <p>
     * Build definition.
     * </p>
     * 
     * @param target
     * @param archive
     * @param extensionClass
     * @param method
     */
    JavaExtensionMethodDefinition(Method method) {
        this.target = method.getParameterTypes()[0];
        this.extensionClass = method.getDeclaringClass();
        this.method = method;

        Path path = I.locate(target);
        this.archive = path == null ? Platform.JavaRuntime : path;
    }
}
