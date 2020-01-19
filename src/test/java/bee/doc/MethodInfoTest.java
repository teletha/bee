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

import java.io.Serializable;
import java.util.List;

import org.junit.jupiter.api.Test;

public class MethodInfoTest extends JavadocTestSupport {

    @Test
    protected void returnType() {
        assert checkReturnType(currentMethod(), "<i>void</i>");
    }

    @Test
    void returnPrimitive() {
        assert checkReturnType(method("integer"), "<i>int</i>");
    }

    protected int integer() {
        return 1;
    }

    @Test
    void returnString() {
        assert checkReturnType(method("string"), "<i>java.lang.String</i>");
    }

    protected String string() {
        return "";
    }

    @Test
    void returnGenerics() {
        assert checkReturnType(method("generics"), "<i>T</i>");
    }

    protected <T> T generics() {
        return null;
    }

    @Test
    void returnBounded() {
        assert checkReturnType(method("bounded"), "<i>T</i>");
    }

    protected <T extends Serializable> T bounded() {
        return null;
    }

    @Test
    void returnParameterized() {
        assert checkReturnType(method("parameterized"), "<i>java.util.List<i class='parameters'><i>java.lang.String</i></i></i>");
    }

    protected List<String> parameterized() {
        return null;
    }

    /**
     * Shortcut method.
     * 
     * @param info
     * @param expected
     * @return
     */
    private boolean checkReturnType(MethodInfo info, String expected) {
        assert sameXML(info.createReturnType(), expected);
        return true;
    }
}
