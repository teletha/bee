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

/**
 * @version 2012/04/18 10:56:08
 */
@SuppressWarnings("bb")
public class Suppresser extends AnnotationValidator<SuppressWarnings> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validate(SuppressWarnings annotation, Source source, AnnotationNotifier notifier) {
        notifier.error("error だoooお ", annotation.value());
    }
}
