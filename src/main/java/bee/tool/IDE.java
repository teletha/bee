/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.tool;

import java.nio.file.Path;

import kiss.Extensible;

/**
 * @version 2012/04/16 22:42:37
 */
public abstract class IDE implements Extensible {

    public abstract void addClassPath(Path classpath);

    public abstract void build(Path root);

    /**
     * @version 2012/04/16 23:57:19
     */
    private static class Eclipse extends IDE {

        /**
         * {@inheritDoc}
         */
        @Override
        public void addClassPath(Path classpath) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Path root) {
        }
    }

    /**
     * @version 2012/04/16 23:57:19
     */
    private static class NetBeans extends IDE {

        /**
         * {@inheritDoc}
         */
        @Override
        public void addClassPath(Path classpath) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Path root) {
        }
    }
}
