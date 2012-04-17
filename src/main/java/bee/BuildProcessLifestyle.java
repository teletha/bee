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

import kiss.I;
import kiss.Prototype;

/**
 * @version 2012/04/17 17:00:43
 */
public class BuildProcessLifestyle<M> extends Prototype<M> {

    /**
     * @param modelClass
     */
    private BuildProcessLifestyle(Class<M> modelClass) {
        super(modelClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public M resolve() {
        Build build = I.make(Build.class);

        return super.resolve();
    }
}
