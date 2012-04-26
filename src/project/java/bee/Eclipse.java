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

import bee.task.Install;

/**
 * @version 2012/04/19 0:48:05
 */
public class Eclipse extends bee.task.Eclipse {

    /**
     * {@inheritDoc}
     */
    @Override
    public void eclipse() {
        require(Install.class).project();
        super.eclipse();
    }
}
