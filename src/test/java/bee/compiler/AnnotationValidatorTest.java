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

import org.junit.Rule;
import org.junit.Test;

import bee.Null;
import bee.task.AnnotationProcessor;
import bee.task.AnnotationValidator;

/**
 * @version 2012/11/10 2:22:26
 */
public class AnnotationValidatorTest {

    @Rule
    public static final PrivateSourceDirectory source01 = new PrivateSourceDirectory("source01");

    @Test
    public void byClass() throws Exception {
        ByClass.invoked = false;

        JavaCompiler compiler = new JavaCompiler(Null.UI);
        compiler.addSourceDirectory(source01.root);
        compiler.setOutput(source01.output);
        compiler.addProcessor(AnnotationProcessor.class);
        compiler.compile();

        assert ByClass.invoked;
    }

    /**
     * @version 2012/11/10 2:22:23
     */
    private static final class ByClass extends AnnotationValidator<SourceAnnotation> {

        private static boolean invoked = false;

        /**
         * {@inheritDoc}
         */
        @Override
        public void validate(SourceAnnotation annotation) {
            assert annotation != null;

            invoked = true;
        }
    }
}
