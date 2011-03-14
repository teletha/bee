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

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import org.junit.Rule;
import org.junit.Test;

import bee.compiler.source01.MainClass;

/**
 * @version 2010/12/19 10:21:32
 */
public class JavaCompilerTest {

    @Rule
    public static final PrivateSourceDirectory source01 = new PrivateSourceDirectory("source01");

    @Test
    public void outputPresent() throws Exception {
        Path source = source01.locateSourceCode(MainClass.class);
        Path bytecode = source01.locateByteCode(MainClass.class);
        assertTrue(Files.exists(source));
        assertTrue(Files.notExists(bytecode));

        Files.createDirectories(source01.output);
        assertTrue(Files.exists(source01.output));

        JavaCompiler compiler = new JavaCompiler();
        compiler.addSourceDirectory(source01.root);
        compiler.setOutput(source01.output);
        compiler.compile();

        assertTrue(Files.exists(source));
        assertTrue(Files.exists(bytecode));
    }

    @Test
    public void outputAbsent() throws Exception {
        Path source = source01.locateSourceCode(MainClass.class);
        Path bytecode = source01.locateByteCode(MainClass.class);
        assertTrue(Files.exists(source));
        assertTrue(Files.notExists(bytecode));

        Files.deleteIfExists(source01.output);
        assertTrue(Files.notExists(source01.output));

        JavaCompiler compiler = new JavaCompiler();
        compiler.addSourceDirectory(source01.root);
        compiler.setOutput(source01.output);
        compiler.compile();

        assertTrue(Files.exists(source));
        assertTrue(Files.exists(bytecode));
    }

    @Test
    public void processor() throws Exception {
        Path source = source01.locateSourceCode(MainClass.class);
        Path bytecode = source01.locateByteCode(MainClass.class);
        assertTrue(Files.exists(source));
        assertTrue(Files.notExists(bytecode));

        Files.deleteIfExists(source01.output);
        assertTrue(Files.notExists(source01.output));

        JavaCompiler compiler = new JavaCompiler();
        compiler.addSourceDirectory(source01.root);
        compiler.setOutput(source01.output);
        compiler.addProcessor(APT.class);
        compiler.addProcessor(APT1.class);
        // compiler.addProcessor(BeeProcessor.class);
        compiler.compile();

        assertTrue(Files.exists(source));
        assertTrue(Files.exists(bytecode));
    }

    /**
     * @version 2011/03/14 17:16:49
     */
    @SupportedSourceVersion(SourceVersion.RELEASE_7)
    @SupportedAnnotationTypes("java.lang.SuppressWarnings")
    public static class APT extends AbstractProcessor {

        private ProcessingEnvironment environment;

        /**
         * {@inheritDoc}
         */
        @Override
        public synchronized void init(ProcessingEnvironment processingEnv) {
            super.init(processingEnv);

            this.environment = processingEnv;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            System.out.println(annotations);
            for (TypeElement annotation : annotations) {
                for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                    System.out.println(element.getAnnotation(SuppressWarnings.class).value()[0]);
                }
            }
            return false;
        }
    }

    /**
     * @version 2011/03/14 17:16:49
     */
    @SupportedSourceVersion(SourceVersion.RELEASE_7)
    @SupportedAnnotationTypes("java.lang.SuppressWarnings")
    public static class APT1 extends AbstractProcessor {

        private ProcessingEnvironment environment;

        /**
         * {@inheritDoc}
         */
        @Override
        public synchronized void init(ProcessingEnvironment processingEnv) {
            super.init(processingEnv);

            this.environment = processingEnv;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            System.out.println("APT1");
            return false;
        }
    }
}
