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

import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.Resource;

import org.junit.Rule;
import org.junit.Test;

import bee.apt.BeeProcessor;
import bee.compiler.source01.MainClass;
import ezunit.PrivateModule;

/**
 * @version 2011/03/22 11:56:05
 */
public class AnnotationValidatorTest {

    @Rule
    public static final PrivateSourceDirectory source = new PrivateSourceDirectory("source01");

    @Rule
    public static final PrivateModule module = new PrivateModule();

    @Test
    public void byClass() throws Exception {
        JavaCompiler compiler = new JavaCompiler();
        compiler.addSourceDirectory(source.root);
        compiler.setOutput(source.output);
        compiler.addProcessor(BeeProcessor.class);
        compiler.compile();
        Path path = source.locateByteCode(MainClass.class);
        System.out.println(Files.size(path));
    }

    /**
     * @version 2011/03/22 11:57:48
     */
    private static final class Check implements AnnotationValidator<Resource> {

        /**
         * @see bee.compiler.AnnotationValidator#validate(java.lang.annotation.Annotation,
         *      java.lang.Class, bee.compiler.Validator)
         */
        @Override
        public void validate(Resource annotation, Class annotatedClass, Validator validator) {
            System.out.println("invoked " + annotation + "  " + (annotatedClass == MainClass.class));
        }
    }
}
