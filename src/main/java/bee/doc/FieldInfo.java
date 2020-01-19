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

import javax.lang.model.element.VariableElement;

import kiss.XML;

public class FieldInfo extends MemberInfo {

    private final XML type;

    /**
     * @param e
     */
    FieldInfo(VariableElement e, TypeResolver resolver) {
        super(e, resolver);

        this.type = parseTypeAsXML(e.asType()).addClass("return");
    }

    /**
     * Build type element.
     * 
     * @return
     */
    public XML createType() {
        return type.clone();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String id() {
        return name;
    }
}
