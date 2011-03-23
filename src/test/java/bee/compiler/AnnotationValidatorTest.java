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

import org.junit.Rule;
import org.junit.Test;

import bee.UserNotifier;
import ezunit.PrivateModule;

/**
 * @version 2011/03/23 18:46:45
 */
public class AnnotationValidatorTest {

    @Rule
    public static final PrivateSourceDirectory source = new PrivateSourceDirectory("source01");

    @Rule
    public static final PrivateModule module = new PrivateModule();

    @Test
    public void byClass() throws Exception {
        ByClass.invoked = false;

        JavaCompiler compiler = new JavaCompiler();
        compiler.addSourceDirectory(source.root);
        compiler.setOutput(source.output);
        compiler.addProcessor(BeeProcessor.class);
        compiler.compile();

        assert ByClass.invoked;
    }

    /**
     * @version 2011/03/23 18:46:42
     */
    private static final class ByClass implements AnnotationValidator<SourceAnnotation> {

        private static boolean invoked = false;

        /**
         * @see bee.compiler.AnnotationValidator#validate(java.lang.annotation.Annotation,
         *      bee.UserNotifier)
         */
        @Override
        public void validate(SourceAnnotation annotation, UserNotifier notifier) {
            assert annotation != null;
            assert notifier != null;
            assert annotation.value().equals("Main");

            invoked = true;
        }
    }
}
