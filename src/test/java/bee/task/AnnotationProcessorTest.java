/*
 * Copyright (C) 2024 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

import org.junit.jupiter.api.Test;

import bee.TaskTestBase;
import bee.sample.Interface;
import bee.util.JavaCompiler;

class AnnotationProcessorTest extends TaskTestBase {

    @Test
    void byClass() throws Exception {
        project.importBy(Interface.class);

        ResourceAwareProcessor.initialized = false;

        JavaCompiler.with()
                .addSourceDirectory(project.getRoot())
                .setOutput(project.getOutput())
                .addProcessor(ResourceAwareProcessor.class)
                .compile();

        assertTrue(ResourceAwareProcessor.initialized);
    }

    @Test
    void byInstance() throws Exception {
        project.importBy(Interface.class);

        ResourceAwareProcessor.initialized = false;

        ResourceAwareProcessor processor = new ResourceAwareProcessor();
        processor.instance = false;

        JavaCompiler.with() //
                .addSourceDirectory(project.getRoot())
                .setOutput(project.getOutput())
                .addProcessor(processor)
                .compile();

        assertTrue(ResourceAwareProcessor.initialized);
        assertTrue(processor.instance);
    }

    /**
     * @version 2011/03/22 10:04:37
     */
    @SupportedSourceVersion(SourceVersion.RELEASE_7)
    @SupportedAnnotationTypes("javax.annotation.Resource")
    public static class ResourceAwareProcessor extends AbstractProcessor {

        private static boolean initialized = false;

        private boolean instance = false;

        /**
         * {@inheritDoc}
         */
        @Override
        public synchronized void init(ProcessingEnvironment environment) {
            initialized = instance = true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment environment) {
            return true;
        }
    }
}