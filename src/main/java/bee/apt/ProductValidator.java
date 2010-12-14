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
package bee.apt;

import bee.Product;

/**
 * @version 2010/06/10 15:18:26
 */
public class ProductValidator implements TypeAnnotationValidator<Product> {

    /**
     * @see bee.apt.TypeAnnotationValidator#validate(java.lang.Class,
     *      java.lang.annotation.Annotation)
     */
    @Override
    public void validate(Class type, Product annotation) throws InvalidValue {
        throw new InvalidValue(annotation.artifact());
    }
}
