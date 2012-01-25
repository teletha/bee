/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package testament;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

import kiss.I;
import sun.reflect.Reflection;
import sun.reflect.ReflectionFactory;

/**
 * @version 2010/02/12 15:53:22
 */
public class UnsafeUtility {

    /**
     * <p>
     * Load the specified class in tools.jar.
     * </p>
     * 
     * @param toolClassName A target tool class name.
     * @return A loaded tool class.
     */
    public static Class getTool(String toolClassName) {
        try {
            // Try loading class directly, in case tool is on the bootclasspath.
            return Class.forName(toolClassName, false, null);
        } catch (ClassNotFoundException ignore) {
            // search JAVA_HOME
            Path home = I.locate(System.getProperty("java.home"));

            if (home.getFileName().toString().equalsIgnoreCase("jre")) {
                home = home.getParent();
            }

            try {
                // load tools.jar
                URLClassLoader loader = new URLClassLoader(new URL[] {home.resolve("lib/tools.jar").toUri().toURL()}, ClassLoader.getSystemClassLoader());

                return Class.forName(toolClassName, false, loader);
            } catch (Exception e) {
                throw I.quiet(e);
            }
        }
    }

    /**
     * <p>
     * Speculate the instantiator class for the instance of this method caller class.
     * </p>
     * <p>
     * <em>NOTE</em>: You should call this method in constructor or instance initializer block.
     * Otherwise, it will cause an unexpected behavior.
     * </p>
     */
    public static Class speculateInstantiator() {
        StackTraceElement[] elements = new Throwable().getStackTrace();

        try {
            Class caller = Class.forName(elements[1].getClassName());

            for (int i = 2; i < elements.length; i++) {
                Class clazz = Class.forName(elements[i].getClassName());

                // check subclass
                if (caller.isAssignableFrom(clazz)) {
                    // check constructor
                    if (elements[i].getMethodName().equals("<init>")) {
                        continue;
                    }
                }

                // API definition
                return clazz;
            }
        } catch (ClassNotFoundException e) {
            throw I.quiet(e);
        }

        // Not found
        throw new IllegalStateException("The suitable caller class is not found.");
    }

    /**
     * Helper method to create new instance of the specified model class bypassing constructor and
     * initializer.
     * 
     * @param <T>
     * @param model
     * @return A constructor bypassed instance.
     */
    public static <T> T newInstance(Class<T> model) {
        if (model == null) {
            throw new IllegalArgumentException("This model class is null.");
        }

        if (model.isInterface()) {
            throw new IllegalArgumentException("This model class represents interface.");
        }

        try {
            return (T) ReflectionFactory.getReflectionFactory()
                    .newConstructorForSerialization(model, Object.class.getConstructor())
                    .newInstance();
        } catch (InstantiationException e) {
            // If this exception will be thrown, it is bug of this program. So we must rethrow the
            // wrapped error in here.
            throw new Error(e);
        } catch (IllegalAccessException e) {
            // If this exception will be thrown, it is bug of this program. So we must rethrow the
            // wrapped error in here.
            throw new Error(e);
        } catch (InvocationTargetException e) {
            // If this exception will be thrown, it is bug of this program. So we must rethrow the
            // wrapped error in here.
            throw new Error(e);
        } catch (NoSuchMethodException e) {
            // If this exception will be thrown, it is bug of this program. So we must rethrow the
            // wrapped error in here.
            throw new Error(e);
        }
    }

    /**
     * Helper method to compute caller class.
     * 
     * @param depth
     * @return A caller object.
     */
    public static Class getCaller(int depth) {
        return Reflection.getCallerClass(depth + 2);
    }
}
