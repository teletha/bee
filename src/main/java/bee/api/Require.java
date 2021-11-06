/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.api;

public abstract class Require {

    /**
     * Delay the resolution of dependencies until they are actually needed.
     * 
     * @param dependencies A list of dependencies.
     */
    protected Require(String... dependencies) {
        for (String dep : dependencies) {
            Repository.require(dep);
        }
    }
}