/*
 * Copyright (C) 2015 Nameless Production Committee
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

/**
 * @version 2011/07/11 16:58:39
 */
@Manageable(lifestyle = Singleton.class)
class UserInterfaceLisfestyle implements Lifestyle<UserInterface> {

    /** The actual store. */
    static final ThreadLocal<UserInterface> local = new InheritableThreadLocal();

    // default setting
    static {
        local.set(new CommandLineUserInterface());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserInterface get() {
        return local.get();
    }
}
