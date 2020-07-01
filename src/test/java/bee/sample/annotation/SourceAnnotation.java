/*
 * Copyright (C) 2020 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.sample.annotation;

/**
 * @version 2011/03/23 18:40:24
 */
public @interface SourceAnnotation {

    String value() default "";
}