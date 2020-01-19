/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.doc;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;

import kiss.XML;

public class MethodInfo extends ExecutableInfo {

    /** The compiled return type expression. */
    private final XML returnType;

    /** The return type flag. */
    private final boolean isVoid;

    /**
     * @param e
     */
    public MethodInfo(ExecutableElement e, TypeResolver resolver) {
        super(e, resolver);

        this.isVoid = e.getReturnType().getKind() == TypeKind.VOID;
        this.returnType = parseTypeAsXML(e.getReturnType());
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public XML createReturnType() {
        return returnType.clone();
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public XML createReturnComment() {
        return returnTag.isPresent() ? returnTag.v.clone() : null;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public boolean returnVoid() {
        return isVoid;
    }
}
