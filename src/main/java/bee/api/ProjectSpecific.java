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

import kiss.I;
import kiss.Prototype;

/**
 * @version 2012/04/17 23:34:37
 */
public class ProjectSpecific<M> extends Prototype<M> {

    /**
     * @param modelClass
     */
    private ProjectSpecific(Class<M> modelClass) {
        super(modelClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public M get() {
        Project project = I.make(Project.class);
        M m = (M) project.associates.get(instantiator.getDeclaringClass());

        if (m == null) {
            m = super.get();
            project.associates.put(instantiator.getDeclaringClass(), m);
        }
        return m;
    }
}
