/*
 * Copyright (C) 2025 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.api;

import java.util.function.Function;

import kiss.I;
import kiss.Lifestyle;

public class ProjectSpecific<M> implements Lifestyle<M> {

    /** The target type. */
    private final Class<M> type;

    /** The target type builder. */
    private final Lifestyle<M> lifestyle;

    /**
     * Create Singleton instance.
     * 
     * @param type A target class.
     */
    public ProjectSpecific(Class<M> type) {
        this.type = type;
        this.lifestyle = I.prototype(type);
    }

    /**
     * Create Singleton instance.
     * 
     * @param type A target class.
     */
    protected ProjectSpecific(Class<M> type, Function<Class<M>, Lifestyle<M>> builder) {
        this.type = type;
        this.lifestyle = builder.apply(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public M call() throws Exception {
        return (M) I.make(Project.class).associates.computeIfAbsent(type, key -> lifestyle.get());
    }
}