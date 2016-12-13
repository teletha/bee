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

import java.util.Arrays;

/**
 * @version 2016/12/13 10:16:04
 */
@Extension
public class Strings {

    public boolean isNotEmpty(int value, String name) {
        try {
            Object[] params = {this, value, name};
            String[] paramNames = {"java.lang.String", "int", "java.lang.String"};
            Class[] paramTypes = new Class[paramNames.length];

            for (int i = 0; i < paramTypes.length; i++) {
                paramTypes[i] = Class.forName(paramNames[i], true, ClassLoader.getSystemClassLoader());
            }
            System.out.println(Arrays.toString(params));
            Class clazz = Class.forName("bee.task.EnhanceLibrary", true, ClassLoader.getSystemClassLoader());
            return ((Boolean) clazz.getMethod("isNotEmpty", paramTypes).invoke(null, params)).booleanValue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
