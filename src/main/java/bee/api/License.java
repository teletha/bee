/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.api;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import kiss.I;

/**
 * @version 2012/10/21 15:16:38
 */
public enum License {

    MIT("http://opensource.org/licenses/mit-license.php");

    /** The year expression. */
    private static final DateFormat YEAR = new SimpleDateFormat("yyyy");

    /** The license uri. */
    private final String uii;

    /**
     * @param uii
     */
    private License(String uii) {
        this.uii = uii;
    }

    /**
     * <p>
     * Write lisence text.
     * </p>
     * 
     * @return
     */
    public List<String> forXML() {
        return text("<!--", " - ", "-->");
    }

    /**
     * <p>
     * Write lisence text.
     * </p>
     * 
     * @return
     */
    public List<String> forJava() {
        return text("/*", " * ", " */");
    }

    /**
     * <p>
     * Write lisence text.
     * </p>
     * 
     * @return
     */
    private List<String> text(String start, String prefix, String end) {
        List<String> license = new ArrayList();
        license.add(start);
        license.add(prefix + "Copyright (C) " + YEAR.format(new Date()) + " " + I.make(Project.class).getProduct() + " Development Team");
        license.add(prefix);
        license.add(prefix + "Licensed under the " + name() + " License (the \"License\");");
        license.add(prefix + "you may not use this file except in compliance with the License.");
        license.add(prefix + "You may obtain a copy of the License at");
        license.add(prefix);
        license.add(prefix + "         " + uri());
        license.add(end);

        return license;
    }

    /**
     * <p>
     * Retrieve lisence URI.
     * </p>
     * 
     * @return A lisence URI.
     */
    protected String uri() {
        return uii;
    }
}
