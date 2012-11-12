/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee;

import bee.api.Command;
import bee.task.AnnotationValidator;

/**
 * @version 2012/11/12 16:09:08
 */
public class CommandValidator extends AnnotationValidator<Command> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validate(Command annotation) {
        warn(annotation.getClass(), "   ", getClassName());
    }
}
