/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.compiler;

import static org.junit.Assert.*;

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

import org.junit.Rule;
import org.junit.Test;

import bee.Null;

/**
 * @version 2011/03/22 10:03:04
 */
public class PluggableAnnotationProcessorTest {

    @Rule
    public static final PrivateSourceDirectory source01 = new PrivateSourceDirectory("source01");

    @Test
    public void byClass() throws Exception {
        ResourceAwareProcessor.initialized = false;

        JavaCompiler compiler = new JavaCompiler(Null.UI);
        compiler.addSourceDirectory(source01.root);
        compiler.setOutput(source01.output);
        compiler.addProcessor(ResourceAwareProcessor.class);
        compiler.compile();

        assertTrue(ResourceAwareProcessor.initialized);
    }

    @Test
    public void byInstance() throws Exception {
        ResourceAwareProcessor.initialized = false;

        ResourceAwareProcessor processor = new ResourceAwareProcessor();
        processor.instance = false;

        JavaCompiler compiler = new JavaCompiler(Null.UI);
        compiler.addSourceDirectory(source01.root);
        compiler.setOutput(source01.output);
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
