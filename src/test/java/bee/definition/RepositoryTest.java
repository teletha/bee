/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.definition;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;

/**
 * @version 2012/03/22 21:01:41
 */
public class RepositoryTest {

    /** The repository. */
    private Repository repository = new Repository();

    private final Set<Library> dependencies = new TreeSet();

    @Before
    public void cleanup() {
        dependencies.clear();
    }

    private void resolve(String qualified, Scope scope) {
        dependencies.addAll(repository.resolve(Collections.singleton(new Library(qualified)), scope));
    }

    private void assertDependency(String qualified) {
        assert dependencies.contains(new Library(qualified));
    }

    @Test
    public void resolve() throws Exception {
        resolve("commons-lang:commons-lang:1.0", Scope.COMPILE);

        assertDependency("junit:junit:3.7");
    }
}
