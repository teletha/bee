/*
 * Copyright (C) 2014 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.api;

import java.util.Set;

import org.junit.Test;

import bee.BlinkProject;
import bee.Null;

/**
 * @version 2014/08/18 16:52:13
 */
public class DependencyTest {

    @Test
    public void empty() {
        BlinkProject project = new BlinkProject();
        Repository repository = new Repository(project, Null.UI);
        Set<Library> dependencies = repository.collectDependency(project, Scope.Compile);
        assert dependencies.size() == 0;
    }

    @Test
    public void testname() {
        BlinkProject project = new BlinkProject();
        project.require("org.objectweb", "asm", "5.0");

        Repository repository = new Repository(project, Null.UI);
        Set<Library> dependencies = repository.collectDependency(project, Scope.Compile);
        assert dependencies.size() == 1;
    }
}
