/*
 * Copyright (C) 2025 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import bee.api.Project;
import kiss.I;
import kiss.Lifestyle;

public class OnProject<T> implements Lifestyle<T> {

    private static final Map<Project, Object> ASSOCIATION = new ConcurrentHashMap();

    private final Lifestyle<T> type;

    /**
     * @param type
     */
    private OnProject(Class<T> type) {
        this.type = I.prototype(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T call() throws Exception {
        return (T) ASSOCIATION.computeIfAbsent(ForProject.local.get(), key -> type.get());
    }
}
