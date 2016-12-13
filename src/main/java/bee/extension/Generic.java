/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.extension;

import java.util.function.Supplier;

/**
 * @version 2016/12/13 12:04:23
 */
public interface Generic<P, R> {

    public default Supplier<R> with2(P param) {
        return null;
    }

    public default Supplier<R> withR(R param) {
        return null;
    }
}
