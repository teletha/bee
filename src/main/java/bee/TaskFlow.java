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

import kiss.Managed;

@Managed(OnProject.class)
class TaskFlow {

    private final Fail failure = new Fail("");

    /**
     * @param e
     */
    void fail(Throwable e) {
        failure.reason(e);
    }

    /**
     * @return
     */
    boolean isFailed() {
        return failure.getSuppressed().length != 0;
    }

}
