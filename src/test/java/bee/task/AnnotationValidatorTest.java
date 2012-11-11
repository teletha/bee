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

import org.junit.Test;

import bee.BlinkProject;
import bee.compiler.source03.root.Enum;

/**
 * @version 2012/11/11 16:26:26
 */
public class AnnotationValidatorTest {

    @Test
    public void validator() throws Exception {
        BlinkProject project = new BlinkProject();
        project.source(Enum.class);
    }
}
