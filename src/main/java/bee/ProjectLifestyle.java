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
import bee.api.Project;

/**
 * @version 2012/03/27 16:16:00
 */
@Manageable(lifestyle = Singleton.class)
class ProjectLifestyle implements Lifestyle<Project> {

    /** The actual store. */
    static final ThreadLocal<Project> local = new InheritableThreadLocal();

    /**
     * {@inheritDoc}
     */
    @Override
    public Project get() {
        return local.get();
    }
}
