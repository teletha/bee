/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.sample;

import bee.sample.annotation.SourceAnnotation;

/**
 * @version 2012/11/12 13:31:37
 */
public class Bean {

    private String name;

    /**
     * Get the name property of this {@link Bean}.
     * 
     * @return The name property.
     */
    @SourceAnnotation
    public String getName() {
        return name;
    }

    /**
     * Set the name property of this {@link Bean}.
     * 
     * @param name The name value to set.
     */
    public void setName(String name) {
        this.name = name;
    }
}
