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

import bee.api.Project;

/**
 * @version 2012/04/16 22:42:37
 */
enum IDE {

    Eclipse {

        /**
         * {@inheritDoc}
         */
        @Override
        boolean exist(Project project) {
            return Files.isReadable(project.getRoot().resolve(".classpath"));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        void create(Project project) {
            new bee.task.Eclipse().eclipse();
        }
    },

    NetBeans {

        /**
         * {@inheritDoc}
         */
        @Override
        boolean exist(Project project) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        void create(Project project) {
        }
    },

    IDEA {

        /**
         * {@inheritDoc}
         */
        @Override
        boolean exist(Project project) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        void create(Project project) {
        }
    };

    /**
     * <p>
     * Analyze environment.
     * </p>
     * 
     * @param project
     * @return
     */
    abstract boolean exist(Project project);

    /**
     * <p>
     * Create environment.
     * </p>
     * 
     * @param project
     * @return
     */
    abstract void create(Project project);
}
