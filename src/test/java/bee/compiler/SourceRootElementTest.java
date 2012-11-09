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

/**
 * @version 2012/11/10 2:22:15
 */
public class SourceRootElementTest {

    @Rule
    public static final PrivateSourceDirectory source03 = new PrivateSourceDirectory("source03");

    @Test
    public void specialTypes() throws Exception {
        JavaCompiler compiler = new JavaCompiler(Null.UI);
        compiler.addSourceDirectory(source03.root);
        compiler.setOutput(source03.output);
        compiler.addProcessor(BeeProcessor.class);
        compiler.compile();
    }

    /**
     * @version 2012/11/10 2:22:11
     */
    @SuppressWarnings("unused")
    private static final class Validation extends AnnotationValidator<SourceAnnotation> {

        /**
         * @see bee.compiler.AnnotationValidator#validate(java.lang.annotation.Annotation, Source,
         *      bee.compiler.AnnotationNotifier)
         */
        @Override
        public void validate(SourceAnnotation annotation, Source source, AnnotationNotifier notifier) {
            assert source.root != null;
        }
    }
}
