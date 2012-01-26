/*
 * Copyright (C) 2011 Nameless Production Committee.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bee.compiler;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;
import javax.tools.StandardLocation;

import kiss.I;

/**
 * @version 2011/09/08 11:56:07
 */
public class Source {

    /** The root tree. */
    private final Element root;

    /** The actual tree. */
    private final Element element;

    /** The element util. */
    private final Elements util;

    /** The file util. */
    private final Filer filer;

    /**
     * @param root
     * @param element
     * @param util
     * @param filer
     */
    Source(Element root, Element element, Elements util, Filer filer) {
        this.root = root;
        this.element = element;
        this.util = util;
        this.filer = filer;
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
