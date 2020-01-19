/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.doc.builder;

import java.util.function.Consumer;

import kiss.I;
import kiss.Tree;
import kiss.XML;
import stylist.Style;
import stylist.StyleDSL;
import stylist.StyleDeclarable;

/**
 * Domain Specific Language for HTML.
 */
public abstract class HTML extends Tree<String, XML> {

    /**
     * 
     */
    public HTML() {
        super((name, id, context) -> {
            return I.xml("<" + name + "/>");
        }, null, (follower, current) -> {
            if (follower instanceof Style) {
                Style style = (Style) follower;
                for (String className : style.className()) {
                    current.addClass(className);
                }
            } else {
                follower.accept(current);
            }
        });
    }

    /**
     * Build HTML.
     */
    protected abstract void declare();

    protected final void $(HTML html) {
        html.declare();

        for (XML xml : html.root) {
            $(xml);
        }
    }

    /**
     * <p>
     * accept node attribute with name.
     * </p>
     * 
     * @param name An attribute name.
     */
    protected final Consumer attr(Object name) {
        return attr(name, null);
    }

    /**
     * <p>
     * accept node attribute with name.
     * </p>
     * 
     * @param name An attribute name.
     */
    protected final Consumer<XML> attr(Object name, Object value) {
        return parent -> {
            if (name != null) {
                String n = String.valueOf(name);

                if (!n.isEmpty()) {
                    parent.attr(n, String.valueOf(value));
                }
            }
        };
    }

    /**
     * <p>
     * accept text node.
     * </p>
     * 
     * @param text A text.
     */
    protected final Consumer<XML> text(Object text) {
        return parent -> {
            parent.append(parent.to().getOwnerDocument().createTextNode(String.valueOf(text)));
        };
    }

    /**
     * Shorthand method to write stylesheet link tag.
     * 
     * @param uri URI to css.
     */
    protected final void stylesheet(String uri) {
        $("link", attr("rel", "stylesheet"), attr("href", uri));
    }

    /**
     * Build CSS file and return the path of the generated file.
     * 
     * @param styles A style definition class to write.
     * @return A path to the generated file.
     */
    protected final void stylesheet(String path, Class<? extends StyleDSL> styles) {
        $("link", attr("rel", "stylesheet"), attr("href", "/" + SiteBuilder.current.buildCSS(path, styles)));
    }

    /**
     * Build CSS file and return the path of the generated file.
     * 
     * @param styles A style definition class to write.
     * @return A path to the generated file.
     */
    protected final void stylesheet(String path, StyleDeclarable styles) {
        $("link", attr("rel", "stylesheet"), attr("href", "/" + SiteBuilder.current.buildCSS(path, styles)));
    }

    /**
     * Shorthand method to write script tag.
     * 
     * @param uri URI to script.
     */
    protected final void script(String uri) {
        $("script", attr("src", uri.startsWith("http") ? uri : "/" + uri));
    }

    /**
     * Build JSONP file and return the path of the generated file.
     * 
     * @param styles A style definition class to write.
     * @return A path to the generated file.
     */
    protected final void script(String path, Object model) {
        $("script", attr("src", "/" + SiteBuilder.current.buildJSONP(path, model)));
    }
}
