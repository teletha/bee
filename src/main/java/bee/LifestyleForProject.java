/*
 * Copyright (C) 2024 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee;

import bee.api.Project;
import kiss.Lifestyle;
import kiss.Managed;
import kiss.Singleton;

@Managed(Singleton.class)
class LifestyleForProject implements Lifestyle<Project> {

    /** The actual store. */
    static final ThreadLocal<Project> local = InheritableThreadLocal.withInitial(ZeroProject::new);

    /**
     * {@inheritDoc}
     */
    @Override
    public Project call() throws Exception {
        return local.get();
    }
}