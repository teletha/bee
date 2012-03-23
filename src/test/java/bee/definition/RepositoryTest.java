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

    private void resolve(String qualified) {
        dependencies.addAll(repository.resolve(Collections.singleton(new Library(qualified)), Scope.Compile));
    }

    private void assertDependency(String qualified) {
        assert dependencies.contains(new Library(qualified));
    }

    @Test
    public void noDependencies() throws Exception {
        resolve("commons-io:commons-io:0.1");

        assert dependencies.size() == 1;
        assertDependency("commons-io:commons-io:0.1");
    }

    @Test
    public void dependencyTest() throws Exception {
        resolve("commons-lang:commons-lang:1.0.1");

        assert dependencies.size() == 1;
        assertDependency("commons-lang:commons-lang:1.0.1");
    }

    @Test
    public void dependencyCompile() throws Exception {
        resolve("org.codehaus.woodstox:woodstox-core-asl:4.1.2");

        assert dependencies.size() == 3;
        assertDependency("javax.xml.stream:stax-api:1.0-2");
        assertDependency("org.codehaus.woodstox:stax2-api:3.1.1");
    }

    @Test
    public void dependencyNest() throws Exception {
        resolve("org.apache.httpcomponents:httpclient:4.0");

        assert dependencies.size() == 3;
        assertDependency("javax.xml.stream:stax-api:1.0-2");
        assertDependency("org.codehaus.woodstox:stax2-api:3.1.1");
    }

}
