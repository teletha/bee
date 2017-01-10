/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import com.google.common.base.Supplier;

import bee.Platform;
import bee.UserInterface;
import bee.api.Project;
import kiss.I;

/**
 * <p>
 * User configuration based on property file.
 * </p>
 * 
 * @version 2017/01/10 10:10:04
 */
public class Config {

    /**
     * <p>
     * Retrieve the configuration of the specified class for project.
     * </p>
     *
     * @param type A configuration type.
     * @return A configuration.
     */
    public static <T> T project(Class<T> type) {
        return config(I.make(Project.class).getRoot().resolve(".bee"), type);
    }

    /**
     * <p>
     * Retrieve the configuration of the specified class for user.
     * </p>
     *
     * @param type A configuration type.
     * @return A configuration.
     */
    public static <T> T user(Class<T> type) {
        return config(Platform.BeeHome.resolve("config").resolve(System.getProperty("user.name")), type);
    }

    /**
     * <p>
     * Retrieve the configuration of the specified class for user.
     * </p>
     *
     * @param type A configuration type.
     * @return A configuration.
     */
    public static <T> T users(Supplier<T> config) {
        return null;
    }

    /**
     * <p>
     * Retrieve the configuration of the specified class for system.
     * </p>
     *
     * @param type A configuration type.
     * @return A configuration.
     */
    public static <T> T system(Class<T> type) {
        return config(Platform.BeeHome.resolve("config").resolve("@system@"), type);
    }

    /**
     * <p>
     * Read configuration.
     * </p>
     * 
     * @param directory A target directory.
     * @param type A configuration type.
     * @return A configuration.
     */
    private static <T> T config(Path directory, Class<T> type) {
        String name = name(type);
        Path file = directory.resolve(name + ".txt");
        Paths.createFile(file);
        I.make(UserInterface.class).talk("Use configuration from [" + file + "].");

        return I.make(type, (proxy, method, args) -> {
            Properties properties = new Properties();
            properties.load(Files.newBufferedReader(file, StandardCharsets.UTF_8));

            String key = method.getName();
            String value = properties.getProperty(key);

            if (value == null || value.isEmpty()) {
                if (method.isDefault()) {
                    value = I.transform(invokeDefaultMethod(proxy, method, args), String.class);
                }

                if (value == null || value.isEmpty()) {
                    String desc = name + " " + key;
                    Description description = method.getAnnotation(Description.class);

                    if (description != null) {
                        desc = description.value();
                    }
                    value = I.make(UserInterface.class).ask(desc);
                }

                properties.setProperty(key, value);
                properties.store(Files.newBufferedWriter(file, StandardCharsets.UTF_8), "Bee local configuration.");
            }
            return I.transform(value, method.getReturnType());
        });
    }

    /**
     * <p>
     * Helper method to invoke default method.
     * </p>
     * 
     * @param proxy
     * @param method
     * @param args
     * @return
     * @throws Throwable
     */
    private static Object invokeDefaultMethod(Object proxy, Method method, Object[] args) throws Throwable {
        Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
        constructor.setAccessible(true);

        Class<?> declaringClass = method.getDeclaringClass();
        int allModes = MethodHandles.Lookup.PUBLIC | MethodHandles.Lookup.PRIVATE | MethodHandles.Lookup.PROTECTED | MethodHandles.Lookup.PACKAGE;
        return constructor.newInstance(declaringClass, allModes)
                .unreflectSpecial(method, declaringClass)
                .bindTo(proxy)
                .invokeWithArguments(args);
    }

    /**
     * <p>
     * Compute the human-readable name from the specified class.
     * </p>
     * 
     * @param clazz
     * @return
     */
    private static String name(Class<?> clazz) {
        Description description = clazz.getAnnotation(Description.class);

        if (description != null) {
            return description.value();
        } else {
            return name(clazz, "").trim();
        }
    }

    /**
     * <p>
     * Name builder.
     * </p>
     * 
     * @param clazz
     * @param name
     * @return
     */
    private static String name(Class clazz, String name) {
        if (clazz == null) {
            return name;
        }
        return name(clazz.getEnclosingClass(), name) + " " + clazz.getSimpleName();
    }

    /**
     * @version 2017/01/10 14:31:37
     */
    @Documented
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Description {

        /**
         * <p>
         * The value description.
         * </p>
         * 
         * @return
         */
        String value();
    }
}
