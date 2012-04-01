/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task.exe;

import java.lang.reflect.Method;

/**
 * @version 2012/03/31 22:59:29
 */
public class Activator {

    /**
     * <p>
     * Application Activation Acession.
     * </p>
     * 
     * @param args
     */
    public static void main(String[] args) throws Exception {
        Class clazz = Class.forName("toybox.Toybox");
        Method main = clazz.getMethod("main", String[].class);

        main.invoke(null, (Object) new String[] {});
    }
}
