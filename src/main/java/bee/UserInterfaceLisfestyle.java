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
 * @version 2011/07/11 16:58:39
 */
class UserInterfaceLisfestyle implements Lifestyle<UserInterface> {

    /** The user interface class. */
    static UserInterface ui;

    /**
     * {@inheritDoc}
     */
    @Override
    public UserInterface resolve() {
        return ui;
    }
}
