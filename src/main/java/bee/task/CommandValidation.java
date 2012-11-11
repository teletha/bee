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

import bee.api.Command;

/**
 * @version 2012/11/09 20:04:11
 */
public class CommandValidation extends AnnotationValidator<Command> {

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate(Command annotation) {
        warn(annotation.value(), "   @@@", "   ", getSourceFile());
    }
}
