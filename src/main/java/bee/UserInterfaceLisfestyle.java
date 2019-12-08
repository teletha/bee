/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee;

import kiss.Lifestyle;
import kiss.Managed;
import kiss.Singleton;

@Managed(value = Singleton.class)
class UserInterfaceLisfestyle implements Lifestyle<UserInterface> {

    /** The actual store. */
    static final ThreadLocal<UserInterface> local = new InheritableThreadLocal();

    // default setting
    static {
        local.set(UserInterface.CLI);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserInterface GET() throws Throwable {
        return local.get();
    }
}
