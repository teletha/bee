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

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.sun.mirror.apt.AnnotationProcessor;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.apt.AnnotationProcessorFactory;
import com.sun.mirror.declaration.AnnotationTypeDeclaration;

/**
 * @version 2011/03/24 19:34:53
 */
public class FAQ implements AnnotationProcessorFactory {

    /**
     * @see com.sun.mirror.apt.AnnotationProcessorFactory#getProcessorFor(java.util.Set,
     *      com.sun.mirror.apt.AnnotationProcessorEnvironment)
     */
    @Override
    public AnnotationProcessor getProcessorFor(Set<AnnotationTypeDeclaration> arg0, AnnotationProcessorEnvironment arg1) {
        return null;
    }

    /**
     * @see com.sun.mirror.apt.AnnotationProcessorFactory#supportedAnnotationTypes()
     */
    @Override
    public Collection<String> supportedAnnotationTypes() {
        return Collections.singleton("javax.annotation.Resource");
    }

    /**
     * @see com.sun.mirror.apt.AnnotationProcessorFactory#supportedOptions()
     */
    @Override
    public Collection<String> supportedOptions() {
        return Collections.EMPTY_LIST;
    }

}
