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

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @version 2016/12/13 12:02:59
 */
@Extension
public class Generics {

    /**
     * <p>
     * Apply parameter partialy.
     * </p>
     * 
     * @param function A target function to apply parameter.
     * @param param A fixed parameter.
     * @return A partial applied function.
     */
    @Extension.Method
    public static <P, R> Supplier<R> with2(Function<P, R> function, P param) {
        return null;
    }

}
