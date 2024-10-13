/*
 * Copyright (C) 2023 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.api;

import org.junit.jupiter.api.Test;

public class LibraryTest {

    @Test
    void jar() {
        Library library = new Library("group", "test", "1.0");
        assert library.getJar().equals("group/test/1.0/test-1.0.jar");
    }

    @Test
    void javadocJar() {
        Library library = new Library("group", "test", "1.0");
        assert library.getJavadocJar().equals("group/test/1.0/test-1.0-javadoc.jar");
    }

    @Test
    void sourceJar() {
        Library library = new Library("group", "test", "1.0");
        assert library.getSourceJar().equals("group/test/1.0/test-1.0-sources.jar");
    }

    @Test
    void pom() {
        Library library = new Library("group", "test", "1.0");
        assert library.getPOM().equals("group/test/1.0/test-1.0.pom");
    }
}
