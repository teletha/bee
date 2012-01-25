/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package testament;

import static testament.Ezunit.*;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import kiss.xml.XMLScanner;

/**
 * @version 2011/03/22 17:23:36
 */
public class SAXBuilderTest {

    /**
     * Test method for {@link kiss.xml2.SAXBuilder#getDocument()}.
     */
    @Test
    public void testGetDocument1() throws SAXException {
        SAXBuilder builder = new SAXBuilder();

        // write xml
        builder.startDocument();
        builder.startElement("", "test", "test", EMPTY_ATTR);
        builder.endElement("", "test", "test");
        builder.endDocument();

        Document document = builder.getDocument();
        assert document != null;
        assert document.getDocumentElement().getLocalName().equals("test");
        assert document.getDocumentElement().getNamespaceURI() == null;
        assert document.getDocumentElement().getPrefix() == null;
    }

    /**
     * Test method for {@link kiss.xml2.SAXBuilder#getDocument()}.
     */
    @Test
    public void testGetDocument2() throws SAXException {
        SAXBuilder builder = new SAXBuilder();

        // write xml
        builder.startDocument();
        builder.startPrefixMapping("o", "s");
        builder.startElement("s", "test", "o:test", EMPTY_ATTR);
        builder.endElement("s", "test", "o:test");
        builder.endDocument();

        Document document = builder.getDocument();
        assert document != null;
        assert document.getDocumentElement().getLocalName().equals("test");
        assert document.getDocumentElement().getNamespaceURI().equals("s");
        assert document.getDocumentElement().getPrefix().equals("o");
    }

    /**
     * Test method for {@link kiss.xml2.SAXBuilder#getDocument()}.
     */
    @Test
    public void testGetDocument3() throws SAXException {
        SAXBuilder builder = new SAXBuilder();
        XMLScanner writer = new XMLScanner();
        writer.setContentHandler(builder);

        // write xml
        writer.startDocument();
        writer.startPrefixMapping("o", "s");
        writer.start("o:test");
        writer.start("o:child");
        writer.end();
        writer.startPrefixMapping("o", "change");
        writer.start("o:child");
        writer.end();
        writer.endPrefixMapping("o");
        writer.start("o:child");
        writer.end();
        writer.end();
        writer.endPrefixMapping("o");
        writer.endDocument();

        // assertion
        Document document = builder.getDocument();
        assert document != null;

        Element root = document.getDocumentElement();
        assert root.getLocalName().equals("test");
        assert root.getNamespaceURI().equals("s");
        assert root.getPrefix().equals("o");

        Element child = (Element) root.getFirstChild();
        assert child.getLocalName().equals("child");
        assert child.getNamespaceURI().equals("s");
        assert child.getPrefix().equals("o");

        child = (Element) child.getNextSibling();
        assert child.getLocalName().equals("child");
        assert child.getNamespaceURI().equals("change");
        assert child.getPrefix().equals("o");

        child = (Element) child.getNextSibling();
        assert child.getLocalName().equals("child");
        assert child.getNamespaceURI().equals("s");
        assert child.getPrefix().equals("o");
    }

    /**
     * Test method for {@link kiss.xml2.SAXBuilder#getDocument()}.
     */
    @Test
    public void testInvalidGetDocument1() throws SAXException {
        SAXBuilder builder = new SAXBuilder();

        Document document = builder.getDocument();
        assert document == null;
    }
}
