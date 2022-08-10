/*
 * Copyright (C) 2022 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.util;

import java.util.List;

import org.junit.jupiter.api.Test;

class SnippetTest {

    @Test
    void parse() {
        String source = """
                package test;

                import org.junit.jupiter.api.Test;

                class SomeTest {

                    /**
                     * Create method.
                     */
                    @Test
                    void method() {
                        assert "test" != null;
                    }

                    /**
                     * First line.
                     * Second line.
                     */
                    @Test
                    void method2() {
                        assert "test1" != null;

                        assert "test2" != null;
                    }
                }
                """;

        List<Snippet> snippets = Snippet.parse(true, source, "Test");
        assert snippets.size() == 2;
        assert snippets.get(0).comment.equals("Create method.");
        assert snippets.get(0).code.equals("""
                assert "test" != null;
                """.trim());

        assert snippets.get(1).comment.equals("First line.\r\nSecond line.");
        assert snippets.get(1).code.equals("""
                assert "test1" != null;

                assert "test2" != null;
                """.trim());
    }
}
