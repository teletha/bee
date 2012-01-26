/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.license;

import kiss.Extensible;

/**
 * @version 2012/01/27 0:37:30
 */
public interface License extends Extensible {

    /**
     * <p>
     * Write lisence text.
     * </p>
     * 
     * @return
     */
    String text();
}
