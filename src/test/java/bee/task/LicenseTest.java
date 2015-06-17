/*
 * Copyright (C) 2015 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import bee.BlinkProject;
import bee.util.HeaderType;
import kiss.I;

/**
 * @version 2015/06/15 16:55:54
 */
public class LicenseTest {

    @Test
    public void slashStar() {
        source("/*");
        source(" * License");
        source(" */");

        expect("/*");
        expect(" * Modified");
        expect(" *");
        expect(" * With Space Line");
        expect(" */");

        validateBy(HeaderType.SlashStar.class);
    }

    /** The source contents. */
    private final List<String> sources = new ArrayList();

    /**
     * <p>
     * Write source file.
     * </p>
     */
    private void source() {
        sources.add("");
    }

    /**
     * <p>
     * Write source file.
     * </p>
     */
    private void source(String line) {
        sources.add(line);
    }

    /** The source contents. */
    private final List<String> expect = new ArrayList();

    /**
     * <p>
     * Write expect file.
     * </p>
     */
    private void expect() {
        expect.add("");
    }

    /**
     * <p>
     * Write expect file.
     * </p>
     */
    private void expect(String line) {
        expect.add(line);
    }

    /**
     * 
     */
    private void validateBy(Class<? extends HeaderType> definition) {
        BlinkProject project = new BlinkProject();
        project.set(new ModifiedLicense());

        License task = new License();
        task.convert(sources, I.make(definition));

        assert sources.size() == expect.size();

        for (int i = 0; i < sources.size(); i++) {
            assert sources.get(i).equals(expect.get(i));
        }
    }

    /**
     * Clean up.
     */
    @Before
    public void clean() {
        sources.clear();
        expect.clear();
    }

    /**
     * @version 2015/06/17 9:22:58
     */
    private static class ModifiedLicense implements bee.api.License {

        /**
         * {@inheritDoc}
         */
        @Override
        public String name() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String uri() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<String> text() {
            List<String> text = new ArrayList();
            text.add("Modified");
            text.add("");
            text.add("With Space Line");

            return text;
        }
    }
}
