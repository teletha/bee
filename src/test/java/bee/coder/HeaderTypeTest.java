/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.coder;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import kiss.I;

/**
 * @version 2018/03/31 21:58:22
 */
public class HeaderTypeTest {

    @Test
    public void header() {
        source("/*");
        source(" * Some");
        source(" */");
        source("public class A {");
        source("}");

        expect("/*");
        expect(" * License");
        expect(" */");
        expect("public class A {");
        expect("}");

        validateBy(StandardHeaderStyle.SlashStar);
    }

    @Test
    public void headerWithBlankLine() {
        source();
        source("/*");
        source(" * Some");
        source(" */");
        source();
        source("public class A {");
        source("}");

        expect();
        expect("/*");
        expect(" * License");
        expect(" */");
        expect();
        expect("public class A {");
        expect("}");

        validateBy(StandardHeaderStyle.SlashStar);
    }

    @Test
    public void emply() {
        validateBy(StandardHeaderStyle.SlashStar);
    }

    @Test
    public void blank() {
        source();
        source();

        expect();
        expect();

        validateBy(StandardHeaderStyle.SlashStar);
    }

    @Test
    public void same() {
        source("/*");
        source(" * License");
        source(" */");
        source("public class A {");
        source("}");

        expect("/*");
        expect(" * License");
        expect(" */");
        expect("public class A {");
        expect("}");

        validateBy(StandardHeaderStyle.SlashStar);
    }

    @Test
    public void noHeader() {
        source("public class A {");
        source("}");

        expect("/*");
        expect(" * License");
        expect(" */");
        expect("public class A {");
        expect("}");

        validateBy(StandardHeaderStyle.SlashStar);
    }

    @Test
    public void noHeaderWithConfusableComment() {
        source("public class A {");
        source("  /*");
        source("   * Confusable Comment");
        source("   */");
        source("}");

        expect("/*");
        expect(" * License");
        expect(" */");
        expect("public class A {");
        expect("  /*");
        expect("   * Confusable Comment");
        expect("   */");
        expect("}");

        validateBy(StandardHeaderStyle.SlashStar);
    }

    @Test
    public void multiLineLicense() {
        source("/*");
        source(" * Some");
        source(" */");
        source("public class A {");
        source("}");

        expect("/*");
        expect(" * Multi");
        expect(" * Line");
        expect(" */");
        expect("public class A {");
        expect("}");

        validateBy(StandardHeaderStyle.SlashStar, Multi.class);
    }

    @Test
    public void blankLineLicense() {
        source("/*");
        source(" * Some");
        source(" */");
        source("public class A {");
        source("}");

        expect("/*");
        expect(" * Blank");
        expect(" *");
        expect(" * Line");
        expect(" */");
        expect("public class A {");
        expect("}");

        validateBy(StandardHeaderStyle.SlashStar, Blank.class);
    }

    @Test
    public void html() {
        source("<!--");
        source("  Some");
        source("-->");
        source("<html/>");

        expect("<!--");
        expect("  License");
        expect("-->");
        expect("<html/>");

        validateBy(StandardHeaderStyle.XML);
    }

    @Test
    public void htmlWithoutHeader() {
        source("<!DOCTYPE html>");
        source("<html/>");

        expect("<!--");
        expect("  License");
        expect("-->");
        expect("<!DOCTYPE html>");
        expect("<html/>");

        validateBy(StandardHeaderStyle.XML);
    }

    @Test
    public void xml() {
        source("<?xml version='1.0' encoding='UTF-8'>");
        source("<!--");
        source("  Some");
        source("-->");
        source("<html/>");

        expect("<?xml version='1.0' encoding='UTF-8'>");
        expect("<!--");
        expect("  Blank");
        expect();
        expect("  Line");
        expect("-->");
        expect("<html/>");

        validateBy(StandardHeaderStyle.XML, Blank.class);
    }

    /** The source contents. */
    private final List<String> codeSource = new ArrayList();

    /**
     * <p>
     * Write source file.
     * </p>
     */
    private void source() {
        codeSource.add("");
    }

    /**
     * <p>
     * Write source file.
     * </p>
     */
    private void source(String line) {
        codeSource.add(line);
    }

    /** The source contents. */
    private final List<String> codeExpect = new ArrayList();

    /**
     * <p>
     * Write expect file.
     * </p>
     */
    private void expect() {
        codeExpect.add("");
    }

    /**
     * <p>
     * Write expect file.
     * </p>
     */
    private void expect(String line) {
        codeExpect.add(line);
    }

    /**
     * 
     */
    private void validateBy(HeaderStyle header) {
        validateBy(header, Single.class);
    }

    /**
     * 
     */
    private void validateBy(HeaderStyle header, Class<? extends bee.api.License> licenseClass) {
        List<String> convert = header.convert(codeSource, I.make(licenseClass));

        if (convert != null) {
            assert convert.size() == codeExpect.size();

            for (int i = 0; i < convert.size(); i++) {
                assert convert.get(i).equals(codeExpect.get(i));
            }
        }
    }

    /**
     * Clean up.
     */
    @BeforeEach
    public void clean() {
        codeSource.clear();
        codeExpect.clear();
    }

    /**
     * @version 2015/06/17 9:22:58
     */
    private static abstract class Base implements bee.api.License {

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
        public String fullName() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String identifier() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<String> text() {
            List<String> body = new ArrayList();
            define(body);
            return body;
        }

        /**
         * <p>
         * Define license body.
         * </p>
         * 
         * @param license
         */
        protected abstract void define(List<String> license);
    }

    /**
     * @version 2015/06/17 16:19:04
     */
    private static class Single extends Base {

        /**
         * {@inheritDoc}
         */
        @Override
        protected void define(List<String> license) {
            license.add("License");
        }
    }

    /**
     * @version 2015/06/17 16:19:04
     */
    private static class Multi extends Base {

        /**
         * {@inheritDoc}
         */
        @Override
        protected void define(List<String> license) {
            license.add("Multi");
            license.add("Line");
        }
    }

    /**
     * @version 2015/06/17 16:19:04
     */
    private static class Blank extends Base {

        /**
         * {@inheritDoc}
         */
        @Override
        protected void define(List<String> license) {
            license.add("Blank");
            license.add("");
            license.add("Line");
        }
    }
}