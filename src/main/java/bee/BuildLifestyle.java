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

/**
 * @version 2012/04/17 16:59:21
 */
class BuildLifestyle implements Lifestyle<Build> {

    /** The actual store. */
    static final ThreadLocal<Build> local = new InheritableThreadLocal();

    /**
     * {@inheritDoc}
     */
    @Override
    public Build resolve() {
        return local.get();
    }
}
