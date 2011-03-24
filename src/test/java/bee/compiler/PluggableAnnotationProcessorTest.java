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

import com.sun.source.util.Trees;

/**
 * @version 2011/03/22 10:03:04
 */
public class PluggableAnnotationProcessorTest {

    @Rule
    public static final PrivateSourceDirectory source01 = new PrivateSourceDirectory("source01");

    @Test
    public void byClass() throws Exception {
        ResourceAwareProcessor.initialized = false;

        JavaCompiler compiler = new JavaCompiler();
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

        JavaCompiler compiler = new JavaCompiler();
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
            System.out.println(Trees.instance(environment));
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
