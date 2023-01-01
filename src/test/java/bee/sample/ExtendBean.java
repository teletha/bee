/*
 * Copyright (C) 2023 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.sample;

import bee.sample.annotation.SourceAnnotation;

/**
 * @version 2012/11/13 12:38:20
 */
public class ExtendBean extends Bean implements Interface {

    @SourceAnnotation
    private int age;

    /**
     * Get the age property of this {@link ExtendBean}.
     * 
     * @return The age property.
     */
    public int getAge() {
        return age;
    }

    /**
     * Set the age property of this {@link ExtendBean}.
     * 
     * @param age The age value to set.
     */
    public void setAge(int age) {
        this.age = age;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getType() {
        return 0;
    }
}