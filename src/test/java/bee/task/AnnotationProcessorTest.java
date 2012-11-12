/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import static org.junit.Assert.*;

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

import org.junit.Test;

import bee.BlinkProject;
import bee.sample.Interface;
import bee.util.JavaCompiler;

/**
 * @version 2012/11/12 12:56:47
 */
public class AnnotationProcessorTest {

    @Test
    public void byClass() throws Exception {
        BlinkProject project = new BlinkProject();
        project.importBy(Interface.class);

        ResourceAwareProcessor.initialized = false;

        JavaCompiler compiler = new JavaCompiler();
        compiler.addSourceDirectory(project.getRoot());
        compiler.setOutput(project.getOutput());
        compiler.addProcessor(ResourceAwareProcessor.class);
        compiler.compile();

        assertTrue(ResourceAwareProcessor.initialized);
    }

    @Test
    public void byInstance() throws Exception {
        BlinkProject project = new BlinkProject();
        project.importBy(Interface.class);

        ResourceAwareProcessor.initialized = false;

        ResourceAwareProcessor processor = new ResourceAwareProcessor();
        processor.instance = false;

        JavaCompiler compiler = new JavaCompiler();
        compiler.addSourceDirectory(project.getRoot());
        compiler.setOutput(project.getOutput());
        compiler.addProcessor(processor);
        compiler.compile();

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
