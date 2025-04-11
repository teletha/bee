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

import kiss.Lifestyle;

public class OnProject<T> implements Lifestyle<T> {

    private final Class<T> type;

    /**
     * @param type
     */
    private OnProject(Class<T> type) {
        this.type = type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T call() throws Exception {
        return ForProject.local.get().associate(type);
    }
}
