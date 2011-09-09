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

import bee.UserNotifier;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import ezbean.I;
import ezbean.model.ClassUtil;

/**
 * @version 2010/04/23 16:09:16
 */
public class BeeProcessor implements Processor {

    /** The processing environment. */
    private ProcessingEnvironment environment;

    /** The message notifier. */
    private Notifier notifier;

    /** The abstract syntax tree. */
    private Trees tree;

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
        this.tree = Trees.instance(environment);
        this.util = environment.getElementUtils();

        I.load(ClassUtil.getArchive(BeeProcessor.class));
    }

    /**
     * @see javax.annotation.processing.Processor#process(java.util.Set,
     *      javax.annotation.processing.RoundEnvironment)
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
        ASTScanner scanner = new ASTScanner();

        for (Element element : round.getRootElements()) {
            scanner.scan(tree.getPath(element), tree);
        }

        for (TypeElement annotationType : annotations) {
            for (Element element : round.getElementsAnnotatedWith(annotationType)) {
                Class annotationClass = I.load(annotationType.toString());
                AnnotationValidator validator = I.find(AnnotationValidator.class, annotationClass);

                if (validator != null) {
                    notifier.element = element;
                    try {
                        for (Class type : ClassUtil.getTypes(environment.getClass())) {
                            notifier.error(type.toString());
                        }
                    } catch (Exception e) {
                        notifier.error(e.toString());
                    }
                    validator.validate(element.getAnnotation(annotationClass), new AST(element, util), notifier);
                }
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

    /**
     * @version 2011/09/08 11:08:25
     */
    private static class ASTScanner extends TreePathScanner<Object, Trees> {

        /**
         * @see com.sun.source.util.TreeScanner#visitClass(com.sun.source.tree.ClassTree,
         *      java.lang.Object)
         */
        @Override
        public Object visitClass(ClassTree classTree, Trees trees) {
            System.out.println("class   " + classTree);
            return super.visitClass(classTree, trees);
        }

        /**
         * @see com.sun.source.util.TreeScanner#visitMethod(com.sun.source.tree.MethodTree,
         *      java.lang.Object)
         */
        @Override
        public Object visitMethod(MethodTree arg0, Trees arg1) {
            return super.visitMethod(arg0, arg1);
        }

        /**
         * @see com.sun.source.util.TreeScanner#visitVariable(com.sun.source.tree.VariableTree,
         *      java.lang.Object)
         */
        @Override
        public Object visitVariable(VariableTree arg0, Trees arg1) {
            return super.visitVariable(arg0, arg1);
        }
    }

    /**
     * @version 2011/03/23 17:02:50
     */
    private static class Notifier implements UserNotifier {

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
         * @see bee.UserNotifier#talk(java.lang.String, java.lang.Object[])
         */
        @Override
        public void talk(String message, Object... params) {
            notifier.printMessage(NOTE, String.format(message, params), element);
        }

        /**
         * @see bee.UserNotifier#warn(java.lang.String, java.lang.Object[])
         */
        @Override
        public void warn(String message, Object... params) {
            notifier.printMessage(WARNING, String.format(message, params), element);
        }

        /**
         * @see bee.UserNotifier#error(java.lang.String, java.lang.Object[])
         */
        @Override
        public void error(String message, Object... params) {
            notifier.printMessage(ERROR, String.format(message, params), element);
        }
    }
}
