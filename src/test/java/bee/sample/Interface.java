/*
 * Copyright (C) 2016 Nameless Production Committee
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
 * @version 2012/11/10 2:00:38
 */
public interface Interface {

    @SourceAnnotation
    int getType();
}
