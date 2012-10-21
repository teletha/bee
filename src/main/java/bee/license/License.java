/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.license;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import kiss.I;
import bee.api.Project;

/**
 * @version 2012/10/21 15:16:38
 */
public enum License {

    MIT {

        /**
         * {@inheritDoc}
         */
        @Override
        protected String uri() {
            return "http://opensource.org/licenses/mit-license.php";
        }
    };

    /** The year expression. */
    private static final DateFormat YEAR = new SimpleDateFormat("yyyy");

    /**
     * <p>
     * Write lisence text.
     * </p>
     * 
     * @return
     */
    private List<String> text() {
        List<String> license = new ArrayList();
        license.add("Copyright (C) " + YEAR.format(new Date()) + " " + I.make(Project.class).getProject());
        license.add("");
        license.add("Licensed under the " + name() + " License (the \"License\");");
        license.add("you may not use this file except in compliance with the License.");
        license.add("You may obtain a copy of the License at");
        license.add("");
        license.add("         " + uri());

        return license;
    }

    /**
     * <p>
     * Write lisence text.
     * </p>
     * 
     * @return
     */
    public List<String> textJava() {
        List<String> license = new ArrayList();
        license.add("/**");

        for (String line : text()) {
            license.add(" * " + line);
        }
        license.add(" */");

        return license;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return I.join(text(), "\r\n");
    }

    /**
     * <p>
     * Retrieve lisence URI.
     * </p>
     * 
     * @return A lisence URI.
     */
    protected abstract String uri();
}
