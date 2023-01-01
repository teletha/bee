/*
 * Copyright (C) 2023 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

import bee.TaskTestBase;
import bee.sample.Bean;
import bee.sample.Enum;
import bee.sample.ExtendBean;
import bee.sample.Interface;
import bee.sample.annotation.SourceAnnotation;
import bee.task.AnnotationProcessor.ProjectInfo;
import bee.util.JavaCompiler;
import kiss.I;
import kiss.model.Model;
import psychopath.File;

class AnnotationValidatorTest extends TaskTestBase {

    @Test
    void classNameForClass() throws Exception {
        project.importBy(Bean.class);

        compileWith(new AnnotationValidator<SourceAnnotation>() {

            /**
             * {@inheritDoc}
             */
            @Override
            protected void validate(SourceAnnotation annotation) {
                assert getClassName().equals(Bean.class.getName());
            }
        });
    }

    @Test
    void classNameForEnum() throws Exception {
        project.importBy(Enum.class);

        compileWith(new AnnotationValidator<SourceAnnotation>() {

            /**
             * {@inheritDoc}
             */
            @Override
            protected void validate(SourceAnnotation annotation) {
                assert getClassName().equals(Enum.class.getName());
            }
        });
    }

    @Test
    void classNameForInterface() throws Exception {
        project.importBy(Interface.class);

        compileWith(new AnnotationValidator<SourceAnnotation>() {

            /**
             * {@inheritDoc}
             */
            @Override
            protected void validate(SourceAnnotation annotation) {
                assert getClassName().equals(Interface.class.getName());
            }
        });
    }

    @Test
    void classNameForAnnotation() throws Exception {
        project.importBy(bee.sample.Annotation.class);

        compileWith(new AnnotationValidator<SourceAnnotation>() {

            /**
             * {@inheritDoc}
             */
            @Override
            protected void validate(SourceAnnotation annotation) {
                assert getClassName().equals(bee.sample.Annotation.class.getName());
            }
        });
    }

    @Test
    void sourceFile() throws Exception {
        File source = project.importBy(Bean.class);

        compileWith(new AnnotationValidator<SourceAnnotation>() {

            /**
             * {@inheritDoc}
             */
            @Override
            protected void validate(SourceAnnotation annotation) {
                try {
                    assert Files.isSameFile(source.asJavaPath(), getSourceFile());
                } catch (IOException e) {
                    throw I.quiet(e);
                }
            }
        });
    }

    @Test
    void document() throws Exception {
        project.importBy(Bean.class);

        compileWith(new AnnotationValidator<SourceAnnotation>() {

            /**
             * {@inheritDoc}
             */
            @Override
            protected void validate(SourceAnnotation annotation) {
                assert getDocument().equals("Getter");
            }
        });
    }

    @Test
    void isSubClassOf() throws Exception {
        project.importBy(ExtendBean.class);

        compileWith(new AnnotationValidator<SourceAnnotation>() {

            /**
             * {@inheritDoc}
             */
            @Override
            protected void validate(SourceAnnotation annotation) {
                assert isSubClassOf(ExtendBean.class);
                assert isSubClassOf(Bean.class);
                assert isSubClassOf(Interface.class);
                assert isSubClassOf(Object.class);
                assert !isSubClassOf(HashMap.class);
                assert !isSubClassOf(Serializable.class);
            }
        });
    }

    /**
     * Compile project sources.
     * 
     * @param project
     */
    private void compileWith(AnnotationValidator validator) {
        TestableProcessor processor = new TestableProcessor(validator);

        JavaCompiler.with()
                .addSourceDirectory(project.getSourceSet())
                .setOutput(project.getClasses())
                .addProcessor(processor)
                .addProcessorOption(new ProjectInfo(project))
                .compile();

        assert processor.isCalled;
    }

    /**
     * @version 2012/11/12 11:19:18
     */
    private static class TestableProcessor<T extends Annotation> extends AnnotationProcessor {

        /** The validator to test. */
        private final AnnotationValidator<T> validator;

        /** The validation target annotation. */
        private final Type annotation;

        /** The flag for confirmation. */
        private boolean isCalled = false;

        /**
         * @param validator
         */
        private TestableProcessor(AnnotationValidator<T> validator) {
            this.validator = validator;
            this.annotation = Model.collectParameters(validator.getClass(), AnnotationValidator.class)[0];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected AnnotationValidator<T> find(Class annotation) {
            if (this.annotation != annotation) {
                return null;
            } else {
                isCalled = true;

                return validator;
            }
        }
    }
}