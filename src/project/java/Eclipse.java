/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */

/**
 * @version 2012/03/21 15:08:01
 */
public class Eclipse extends bee.task.Eclipse {

    /**
     * {@inheritDoc}
     */
    @Override
    public void eclipse() {
        super.eclipse();

        System.out.println("override");
    }
}
