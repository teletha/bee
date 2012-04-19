/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.api;

/**
 * @version 2012/04/19 13:47:31
 */
public @interface ProjectDefinition {

    String group();

    String name();

    String version();
}
