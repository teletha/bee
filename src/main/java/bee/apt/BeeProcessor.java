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

import java.util.Collections;
import java.util.Set;

import javax.annotation.processing.Completion;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

import ezbean.I;
import ezbean.model.ClassUtil;

/**
 * @version 2010/04/23 16:09:16
 */
public class BeeProcessor implements Processor {

    // initialization
    static {
        I.load(ClassUtil.getArchive(BeeProcessor.class));
    }

    /** The processing environment. */
    private ProcessingEnvironment process;

    /**
     * @see javax.annotation.processing.Processor#getSupportedOptions()
     */
    @Override
    public Set<String> getSupportedOptions() {
        return Collections.singleton("*");
    }

    /**
     * @see javax.annotation.processing.Processor#getSupportedAnnotationTypes()
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton("*");
    }

    /**
     * @see javax.annotation.processing.Processor#getSupportedSourceVersion()
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_6;
    }

    /**
     * @see javax.annotation.processing.Processor#init(javax.annotation.processing.ProcessingEnvironment)
     */
    @Override
    public void init(ProcessingEnvironment process) {
        this.process = process;
    }

    /**
     * @see javax.annotation.processing.Processor#process(java.util.Set,
     *      javax.annotation.processing.RoundEnvironment)
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
        for (TypeElement annotationType : annotations) {
            try {
                Class annotationClass = Class.forName(annotationType.getQualifiedName().toString());

                for (Element element : round.getElementsAnnotatedWith(annotationType)) {
                    switch (element.getKind()) {
                    case ANNOTATION_TYPE:
                    case CLASS:
                    case ENUM:
                    case INTERFACE:
                        TypeElement type = (TypeElement) element;
                        TypeAnnotationValidator typeAnnotationValidator = I.find(TypeAnnotationValidator.class, annotationClass);

                        Class annotatedClass = Class.forName(type.getQualifiedName().toString());
                        if (typeAnnotationValidator != null) {
                            try {
                                typeAnnotationValidator.validate(annotatedClass, element.getAnnotation(annotationClass));
                            } catch (InvalidValue e) {
                                process.getMessager().printMessage(Kind.ERROR, e.getMessage(), element);
                            } catch (Exception e) {
                                process.getMessager().printMessage(Kind.ERROR, e.getMessage(), element);
                            }
                        }
                        break;

                    case CONSTRUCTOR:

                        break;

                    case ENUM_CONSTANT:
                    case FIELD:
                        break;

                    case LOCAL_VARIABLE:
                    case PARAMETER:
                        break;

                    case METHOD:
                        break;

                    case PACKAGE:
                        break;

                    case EXCEPTION_PARAMETER:
                    case INSTANCE_INIT:
                    case OTHER:
                    case STATIC_INIT:
                    case TYPE_PARAMETER:
                        break;
                    }
                }
            } catch (Exception e) {
                process.getMessager().printMessage(Kind.ERROR, e.getMessage());
            }
        }

        return true;
    }

    /**
     * @see javax.annotation.processing.Processor#getCompletions(javax.lang.model.element.Element,
     *      javax.lang.model.element.AnnotationMirror, javax.lang.model.element.ExecutableElement,
     *      java.lang.String)
     */
    @Override
    public Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation, ExecutableElement member, String userText) {
        return Collections.emptyList();
    }
}
