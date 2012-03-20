/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.cli;

/**
 * @version 2012/02/29 18:57:08
 */
public interface Command<T> {

    void execute(T context);
}
