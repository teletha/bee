/*
 * Copyright (C) 2018 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

import kiss.I;

/**
 * @version 2018/03/28 8:24:50
 */
public class BeeClassLoader extends URLClassLoader {

    /**
     * 
     */
    BeeClassLoader() {
        super(new URL[0], ClassLoader.getSystemClassLoader());
    }

    public void add(Path path) {
        try {
            addURL(path.toUri().toURL());
        } catch (MalformedURLException e) {
            throw I.quiet(e);
        }
    }
}
