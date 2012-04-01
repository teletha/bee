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
package bee.compiler;

import static javax.tools.Diagnostic.Kind.*;

import java.util.Collections;
import java.util.Set;

import javax.annotation.processing.Completion;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import kiss.I;
import kiss.model.ClassUtil;
import bee.UserNotifier;

/**
 * @version 2010/04/23 16:09:16
 */
public class BeeProcessor implements Processor {

    /** The processing environment. */
    private ProcessingEnvironment environment;

    /** The message notifier. */
    private Notifier notifier;

    /** The file utility. */
    private Filer filer;

    /** The utility. */
    private Elements util;

    /**
     * @see javax.annotation.processing.Processor#getSupportedOptions()
     */
    @Override
    public Set<String> getSupportedOptions() {
        return Collections.EMPTY_SET;
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
        return SourceVersion.RELEASE_7;
    }

    /**
     * @see javax.annotation.processing.Processor#init(javax.annotation.processing.ProcessingEnvironment)
     */
    @Override
    public void init(ProcessingEnvironment environment) {
        this.environment = environment;
        this.notifier = new Notifier(environment.getMessager());
        this.filer = environment.getFiler();
        this.util = environment.getElementUtils();

        I.load(ClassUtil.getArchive(BeeProcessor.class));
    }

    /**
     * @see javax.annotation.processing.Processor#process(java.util.Set,
     *      javax.annotation.processing.RoundEnvironment)
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
        Element root = null;
        Set<? extends Element> roots = round.getRootElements();

        if (roots.size() == 1) {
            root = roots.iterator().next();
        }

        try {
            for (TypeElement annotationType : annotations) {
                for (Element element : round.getElementsAnnotatedWith(annotationType)) {
                    Class annotationClass = Class.forName(annotationType.toString());
                    AnnotationValidator validator = I.find(AnnotationValidator.class, annotationClass);

                    if (validator != null) {
                        notifier.element = element;

                        validator.validate(element.getAnnotation(annotationClass), new Source(root, element, util, filer), notifier);
                    }
                }
            }
            return true;
        } catch (ClassNotFoundException e) {
            throw I.quiet(e);
        }
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

    /**
     * @version 2011/03/23 17:02:50
     */
    private static class Notifier extends UserNotifier {

        /** The actual notifier. */
        private Messager notifier;

        /** The current processing element. */
        private Element element;

        /** The current processing annotation. */
        private AnnotationMirror annotation;

        /** The current processing annotation value. */
        private AnnotationValue value;

        /**
         * Private constructor.
         */
        private Notifier(Messager notifier) {
            this.notifier = notifier;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void write(String message) {
            // do nothing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void talk(Object... messages) {
            notifier.printMessage(NOTE, build(messages), element);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void warn(Object... messages) {
            notifier.printMessage(WARNING, build(messages), element);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void error(Object... messages) {
            notifier.printMessage(ERROR, build(messages), element);
        }
    }
}
