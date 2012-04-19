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
 * @version 2012/04/19 11:53:53
 */
public class SourceTest {

    @Rule
    public static final PrivateSourceDirectory source = new PrivateSourceDirectory("source01");

    @Test
    public void source() throws Exception {
        JavaCompiler compiler = new JavaCompiler(Null.UI);
        compiler.addSourceDirectory(source.root);
        compiler.setOutput(source.output);
        compiler.addProcessor(BeeProcessor.class);
        compiler.compile();

        assert ByClass.source != null;
        assert ByClass.source.getSourceFile().startsWith(source.root.toAbsolutePath());
        assert ByClass.source.getClassFile().startsWith(source.output.toAbsolutePath());
    }

    /**
     * @version 2011/03/23 18:46:42
     */
    private static final class ByClass extends AnnotationValidator<SourceAnnotation> {

        private static Source source;

        /**
         * @see bee.compiler.AnnotationValidator#validate(java.lang.annotation.Annotation, Source,
         *      bee.compiler.AnnotationNotifier)
         */
        @Override
        public void validate(SourceAnnotation annotation, Source source, AnnotationNotifier notifier) {
            ByClass.source = source;
        }
    }
}
