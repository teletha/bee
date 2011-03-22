/*
 * Copyright (C) 2011 Nameless Production Committee.
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
package bee.compiler;

import java.lang.annotation.Annotation;

import ezbean.Extensible;

/**
 * @version 2011/03/22 11:28:28
 */
public interface AnnotationValidator<A extends Annotation> extends Extensible {

    /**
     * <p>
     * Validate an annotation value.
     * </p>
     * 
     * @param annotation An target annotation to validate.
     * @param annotatedClass An annotated class.
     * @param validator A current validation context.
     */
    void validate(A annotation, Class annotatedClass, Validator validator);
}
