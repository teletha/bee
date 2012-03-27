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

import kiss.Lifestyle;
import kiss.Manageable;
import kiss.Singleton;
import bee.definition.Project;

/**
 * @version 2012/03/27 16:16:00
 */
@Manageable(lifestyle = Singleton.class)
class ProjectLifestyle implements Lifestyle<Project> {

    /** The current processing project. */
    static Project project;

    /**
     * {@inheritDoc}
     */
    @Override
    public Project resolve() {
        return project;
    }
}
