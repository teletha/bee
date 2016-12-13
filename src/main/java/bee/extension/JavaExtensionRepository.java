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
import java.lang.reflect.Modifier;
import java.nio.file.Path;

import bee.Platform;
import kiss.ClassListener;
import kiss.Events;
import kiss.Manageable;
import kiss.Singleton;
import kiss.Table;

/**
 * @version 2016/12/13 10:23:48
 */
@Manageable(lifestyle = Singleton.class)
class JavaExtensionRepository implements ClassListener<Extension> {

    /** The repository for the extension methods. */
    final Table<Path, JavaExtensionMethodDefinition> methods = new Table();

    /**
     * <p>
     * Check whether some extension exist or not.
     * </p>
     * 
     * @return A result.
     */
    public boolean hasExtension() {
        return methods.isEmpty() == false;
    }

    /**
     * <p>
     * Check whether JRE extension exist or not.
     * </p>
     * 
     * @return A result.
     */
    public boolean hasJREExtension() {
        return methods.containsKey(Platform.JavaRuntime);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load(Class<Extension> clazz) {
        // Collect all extension methods.
        Events.from(clazz.getMethods())
                .take(this::validateExtensionMethod)
                .map(JavaExtensionMethodDefinition::new)
                .toTable(methods, m -> m.archive);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unload(Class<Extension> clazz) {
    }

    /**
     * <p>
     * Validate method as extension.
     * </p>
     * 
     * @param method A method to validate.
     * @return A result.
     */
    private boolean validateExtensionMethod(Method method) {
        // marker annotation
        if (method.isAnnotationPresent(Extension.Method.class) == false) {
            return false;
        }

        // public static modifier
        int modifier = method.getModifiers();

        if (Modifier.isPublic(modifier) == false || Modifier.isStatic(modifier) == false) {
            return false;
        }

        // parameter type
        Class[] types = method.getParameterTypes();

        if (types.length == 0 || types[0].isPrimitive()) {
            return false;
        }

        return true; // This is valid extension method.
    }
}
