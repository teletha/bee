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

import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;

/**
 * @version 2011/03/22 8:53:11
 */
public class PrivateModuleTest {

    @Rule
    public static final PrivateModule module = new PrivateModule(true, false);

    @Rule
    public static final PrivateModule moduleJar = new PrivateModule(true, true);

    @Test
    public void path() throws Exception {
        assert Files.isDirectory(module.path);
        assert Files.isRegularFile(moduleJar.path);
    }

    @Test
    public void convert() throws Exception {
        assert module.convert(Clazz.class) != null;
        assert moduleJar.convert(Clazz.class) != null;
        assert Clazz.class != module.convert(Clazz.class);
        assert Clazz.class != moduleJar.convert(Clazz.class);
    }

    /**
     * @version 2010/11/07 22:53:30
     */
    private static class Clazz {
    }
}
