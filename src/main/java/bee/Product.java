/*
 * Copyright (C) 2010 Nameless Production Committee.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bee;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @version 2010/05/14 16:17:20
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Product {

    /**
     * <p>
     * The group name of this product. Default value is equivalent to the fully qualified package
     * name of the annotated class.
     * </p>
     * 
     * @return A group name.
     */
    String group() default "";

    /**
     * <p>
     * The artifact name of this product.
     * </p>
     * 
     * @return An artifact name.
     */
    String artifact() default "";

    /**
     * <p>
     * The version number of this product.
     * </p>
     * 
     * @return A version number.
     */
    String version();

    /**
     * The qualified identifier of this product. Default value is empty.
     * 
     * @return A qualified identifier.
     */
    String identifier() default "";

    String[] dependency() default {};
}
