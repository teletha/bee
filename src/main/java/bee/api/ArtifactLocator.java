/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.api;

import java.nio.file.Path;

/**
 * @version 2012/04/10 13:39:38
 */
public interface ArtifactLocator {

    /**
     * <p>
     * Product codes.
     * </p>
     */
    ArtifactLocator Jar = new ArtifactLocator() {

        /**
         * {@inheritDoc}
         */
        @Override
        public Path in(Project project) {
            return project.getOutput().resolve(project.getProduct() + "-" + project.getVersion() + ".jar");
        }
    };

    /**
     * <p>
     * </p>
     * 
     * @param project
     * @return
     */
    Path in(Project project);
}
