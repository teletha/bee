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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;
import javax.tools.StandardLocation;

import kiss.I;

/**
 * @version 2012/04/19 12:16:18
 */
public class Source {

    /** The root tree. */
    public final Element root;

    /** The actual tree. */
    public final Element element;

    /** The element util. */
    private final Elements util;

    /**
     * <p>
     * The file util.
     * </p>
     * <p>
     * Eclipse supports only {@value StandardLocation#CLASS_OUTPUT} and
     * {@value StandardLocation#SOURCE_OUTPUT}.
     * </p>
     */
    private final Filer filer;

    /**
     * @param element
     * @param util
     * @param filer
     */
    Source(Element element, Elements util, Filer filer, Map<String, String> options) {
        this.element = element;
        this.util = util;
        this.filer = filer;

        root: while (true) {
            switch (element.getKind()) {
            case CLASS:
            case ANNOTATION_TYPE:
            case ENUM:
            case INTERFACE:
                break root;

            default:
                element = element.getEnclosingElement();
                break;
            }
        }
        this.root = element;
    }

    /**
     * <p>
     * Compute source file path.
     * </p>
     * 
     * @return A path to source file.
     */
    public Path getSourceFile() {
        try {
            String fqcn = root.toString().replace('.', '/').concat(".java");

            return Paths.get(filer.getResource(StandardLocation.SOURCE_OUTPUT, "", fqcn).toUri());
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * Compute class file path.
     * </p>
     * 
     * @return A path to class file.
     */
    public Path getClassFile() {
        try {
            String fqcn = root.toString().replace('.', '/').concat(".class");

            return Paths.get(filer.getResource(StandardLocation.CLASS_OUTPUT, "", fqcn).toUri());
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * Returns the text of the documentation ("Javadoc") comment of an element.
     * </p>
     * <p>
     * A documentation comment of an element is a comment that begins with "/**" , ends with a
     * separate "* /", and immediately precedes the element, ignoring white space. Therefore, a
     * documentation comment contains at least three"*" characters. The text returned for the
     * documentation comment is a processed form of the comment as it appears in source code. The
     * leading "/**" and trailing "* /" are removed. For lines of the comment starting after the
     * initial "/**", leading white space characters are discarded as are any consecutive "*"
     * characters appearing after the white space or starting the line. The processed lines are then
     * concatenated together (including line terminators) and returned.
     * </p>
     * 
     * @return A documentation comment of the source code, or empty string if there is none
     */
    public String getDocument() {
        String doc = util.getDocComment(element);

        return doc == null ? "" : doc.trim();
    }
}
