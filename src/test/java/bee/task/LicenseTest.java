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

import org.junit.Test;

/**
 * @version 2015/06/15 16:55:54
 */
public class LicenseTest {

    @Test
    public void java() {
        line("/*");
        line(" * HEADER");
        line(" */");
        line("package test.org");
        line();
        line("/*");
        line(" * Class Desciption");
        line(" */");
        line("public class Test {");
        line("}");
    }

    /** The source contents. */
    private final List<String> sources = new ArrayList();

    /**
     * <p>
     * Write source file.
     * </p>
     */
    private void line() {

    }

    /**
     * <p>
     * Write source file.
     * </p>
     */
    private void line(String line) {

    }
}
