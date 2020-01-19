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

import org.junit.jupiter.api.Test;

import kiss.XML;
import kiss.Ⅱ;

public class DocumentInfoTest extends JavadocTestSupport {

    /**
     * Text
     */
    @Test
    public void text() {
        ExecutableInfo info = currentMethod();
        assert sameXML(info.comment, "<span class='A'>Text</span>");
    }

    /**
     * <p>
     * Text
     * </p>
     */
    @Test
    public void element() {
        ExecutableInfo info = currentMethod();
        assert sameXML(info.comment, "<span class='A'><p>Text</p></span>");
    }

    /**
     * <a type="stylesheet" href="test.css"/>
     */
    @Test
    public void attribute() {
        ExecutableInfo info = currentMethod();
        assert sameXML(info.comment, "<span class='A'><a href=\"test.css\" type=\"stylesheet\"/></span>");
    }

    /**
     * @param name1 Description.
     * @param name2 This is <em>NOT</em> error.
     */
    @Test
    public void paramTag() {
        ExecutableInfo info = currentMethod();
        Ⅱ<String, XML> param = info.paramTags.get(0);
        assert param.ⅰ.equals("name1");
        assert sameXML(param.ⅱ, "<span>Description.</span>");

        param = info.paramTags.get(1);
        assert param.ⅰ.equals("name2");
        assert sameXML(param.ⅱ, "<span>This is <em>NOT</em> error.</span>");
    }

    /**
     * @param <T> Description.
     * @param <NEXT> This is <em>NOT</em> error.
     */
    @Test
    public void parameterTypeTag() {
        ExecutableInfo info = currentMethod();
        Ⅱ<String, XML> param = info.typeParameterTags.get(0);
        assert param.ⅰ.equals("T");
        assert sameXML(param.ⅱ, "<span>Description.</span>");

        param = info.typeParameterTags.get(1);
        assert param.ⅰ.equals("NEXT");
        assert sameXML(param.ⅱ, "<span>This is <em>NOT</em> error.</span>");
    }

    /**
     * @throws NullPointerException If param is null.
     * @throws IllegalArgumentException If param is <code>0</code>.
     */
    @Test
    public void throwsTag() {
        ExecutableInfo info = currentMethod();
        Ⅱ<String, XML> param = info.throwsTags.get(0);
        assert param.ⅰ.equals("NullPointerException");
        assert sameXML(param.ⅱ, "<span>If param is null.</span>");

        param = info.throwsTags.get(1);
        assert param.ⅰ.equals("IllegalArgumentException");
        assert sameXML(param.ⅱ, "<span>If param is <code>0</code>.</span>");
    }

    /**
     * @return description
     */
    @Test
    public void returnTag() {
        ExecutableInfo info = currentMethod();
        XML description = info.returnTag.exact();
        assert sameXML(description, "<span>description</span>");
    }

    /**
     * {@literal 0 < i}
     */
    @Test
    public void literalTag() {
        ExecutableInfo info = currentMethod();
        assert sameXML(info.comment, "<span class='A'>0 &amp;lt; i</span>");
    }

    /**
     * {@link DocumentInfoTest}
     */
    @Test
    public void linkTagInternalType() {
        ExecutableInfo info = currentMethod();
        assert sameXML(info.comment, "<span class='A'><a href='/types/bee.doc.DocumentInfoTest.html'>DocumentInfoTest</a></span>");
    }

    /**
     * {@link #linkTagInternalMethod()}
     */
    @Test
    public void linkTagInternalMethod() {
        ExecutableInfo info = currentMethod();
        assert sameXML(info.comment, "<span class='A'><a href='/types/bee.doc.DocumentInfoTest.html#linkTagInternalMethod()'>#linkTagInternalMethod()</a></span>");
    }

    /**
     * {@link DocumentInfoTest#linkTagInternalTypeAndMethod()}
     */
    @Test
    public void linkTagInternalTypeAndMethod() {
        ExecutableInfo info = currentMethod();
        assert sameXML(info.comment, "<span class='A'><a href='/types/bee.doc.DocumentInfoTest.html#linkTagInternalTypeAndMethod()'>DocumentInfoTest#linkTagInternalTypeAndMethod()</a></span>");
    }

    /**
     * {@link String}
     */
    @Test
    public void linkTagUnregisteredExternalType() {
        ExecutableInfo info = currentMethod();
        assert sameXML(info.comment, "<span class='A'>String</span>");
    }

    /**
     * {@link String#chars()}
     */
    @Test
    public void linkTagUnregisteredExternalTypeAndMethod() {
        ExecutableInfo info = currentMethod();
        assert sameXML(info.comment, "<span class='A'>String#chars()</span>");
    }

    /**
     * {@linkplain DocumentInfoTest}
     */
    @Test
    public void linkplainTagInternalType() {
        ExecutableInfo info = currentMethod();
        assert sameXML(info.comment, "<span class='A'><a href='/types/bee.doc.DocumentInfoTest.html'>DocumentInfoTest</a></span>");
    }

    /**
     * {@linkplain #linkplainTagInternalMethod()}
     */
    @Test
    public void linkplainTagInternalMethod() {
        ExecutableInfo info = currentMethod();
        assert sameXML(info.comment, "<span class='A'><a href='/types/bee.doc.DocumentInfoTest.html#linkplainTagInternalMethod()'>#linkplainTagInternalMethod()</a></span>");
    }

    /**
     * {@linkplain DocumentInfoTest#linkplainTagInternalTypeAndMethod()}
     */
    @Test
    public void linkplainTagInternalTypeAndMethod() {
        ExecutableInfo info = currentMethod();
        assert sameXML(info.comment, "<span class='A'><a href='/types/bee.doc.DocumentInfoTest.html#linkplainTagInternalTypeAndMethod()'>DocumentInfoTest#linkplainTagInternalTypeAndMethod()</a></span>");
    }

    /**
     * {@linkplain String}
     */
    @Test
    public void linkplainTagUnregisteredExternalType() {
        ExecutableInfo info = currentMethod();
        assert sameXML(info.comment, "<span class='A'>String</span>");
    }

    /**
     * {@linkplain String#chars()}
     */
    @Test
    public void linkplainTagUnregisteredExternalTypeAndMethod() {
        ExecutableInfo info = currentMethod();
        assert sameXML(info.comment, "<span class='A'>String#chars()</span>");
    }

    /**
     * @author Me
     * @author <b>You</b>
     */
    @Test
    public void authorTag() {
        ExecutableInfo info = currentMethod();
        assert sameXML(info.authorTags.get(0), "<span>Me</span>");
        assert sameXML(info.authorTags.get(1), "<span><b>You</b></span>");
    }

    /**
     * @see Text
     * @see <b>String</b>
     */
    @Test
    public void seeTag() {
        ExecutableInfo info = currentMethod();
        assert sameXML(info.seeTags.get(0), "<span>Text</span>");
        assert sameXML(info.seeTags.get(1), "<span><b>String</b></span>");
    }

    /**
     * @since 1.0
     * @since <b>1.2</b>
     */
    @Test
    public void sinceTag() {
        ExecutableInfo info = currentMethod();
        assert sameXML(info.sinceTags.get(0), "<span>1.0</span>");
        assert sameXML(info.sinceTags.get(1), "<span><b>1.2</b></span>");
    }

    /**
     * @version 1.0
     * @version <b>1.2</b>
     */
    @Test
    public void versionTags() {
        ExecutableInfo info = currentMethod();
        assert sameXML(info.versionTags.get(0), "<span>1.0</span>");
        assert sameXML(info.versionTags.get(1), "<span><b>1.2</b></span>");
    }
}
