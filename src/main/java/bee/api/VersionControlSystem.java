/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.api;

/**
 * @version 2017/01/10 3:12:58
 */
public interface VersionControlSystem {

    /** The uri. */
    String uri();

    /** The issue tracker uri. */
    String issue();
}
