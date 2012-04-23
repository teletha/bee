/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @version 2012/04/16 22:42:37
 */
enum IDE {

    Eclipse {

        /**
         * {@inheritDoc}
         */
        @Override
        boolean exist(Path directory) {
            return Files.isReadable(directory.resolve(".classpath"));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        void create(Path directory) {
        }
    },

    NetBeans {

        /**
         * {@inheritDoc}
         */
        @Override
        boolean exist(Path directory) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        void create(Path directory) {
        }
    },

    IDEA {

        /**
         * {@inheritDoc}
         */
        @Override
        boolean exist(Path directory) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        void create(Path directory) {
        }
    };

    /**
     * <p>
     * Analyze environment.
     * </p>
     * 
     * @param directory
     * @return
     */
    abstract boolean exist(Path directory);

    /**
     * <p>
     * Create environment.
     * </p>
     * 
     * @param directory
     * @return
     */
    abstract void create(Path directory);
}
