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

/**
 * @version 2016/12/13 10:16:04
 */
@Extension
public class Strings {

    public boolean isNotEmpty(int value, String name) {
        return ((Boolean) $call$("bee.extension.Strings", "isNotEmpty", new String[] {"java.lang.String", "int",
                "java.lang.String"}, new Object[] {this, value, name})).booleanValue();
    }

    private Object $call$(String extensionClassName, String extensionMethodName, String[] paramNames, Object[] params) {
        try {
            Class[] paramTypes = new Class[paramNames.length];

            for (int i = 0; i < paramTypes.length; i++) {
                paramTypes[i] = $class$(paramNames[i]);
            }
            return $class$(extensionClassName).getMethod(extensionMethodName, paramTypes).invoke(null, params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Class $class$(String name) throws ClassNotFoundException {
        return Class.forName(name, true, ClassLoader.getSystemClassLoader());
    }

    public boolean isNotEmpty2(int value, String name) {
        try {
            String[] paramNames = {"java.lang.String", "int", "java.lang.String"};
            Class[] paramTypes = new Class[paramNames.length];

            for (int i = 0; i < paramTypes.length; i++) {
                paramTypes[i] = Class.forName(paramNames[i], true, ClassLoader.getSystemClassLoader());
            }

            Class clazz = Class.forName("bee.task.EnhanceLibrary", true, ClassLoader.getSystemClassLoader());
            return ((Boolean) clazz.getMethod("isNotEmpty", paramTypes).invoke(null, this, value, name)).booleanValue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param value
     * @return
     */
    @Extension.Method
    public static boolean isNotEmpty(String value) {
        return !value.isEmpty();
    }

    /**
     * @param value
     * @return
     */
    @Extension.Method
    public static boolean isBlank(String value) {
        return value.length() == 0;
    }

    /**
     * @param value
     * @return
     */
    @Extension.Method
    public static boolean isColorName(String value) {
        return value.length() == 0;
    }
}
