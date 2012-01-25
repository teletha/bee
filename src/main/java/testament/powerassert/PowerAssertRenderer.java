/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package testament.powerassert;

import kiss.Extensible;

/**
 * @version 2012/01/22 15:59:30
 */
public interface PowerAssertRenderer<T> extends Extensible {

    /**
     * <p>
     * Render the specified value for human.
     * </p>
     * 
     * @param value A target.
     * @return A human-readable description.
     */
    String render(T value);
}
