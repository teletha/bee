/*
 * Copyright (C) 2023 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import org.eclipse.aether.graph.DependencyNode;

import bee.Task;
import bee.api.Command;
import bee.api.Repository;
import kiss.I;

public class Dependency extends Task {

    @Command("Display the dependency tree.")
    public void tree() {
        Repository repo = I.make(Repository.class);

        DependencyNode dependencies = repo.buildDependencyGraph(project);
        System.out.println(dependencies);

        for (DependencyNode child : dependencies.getChildren()) {
            System.out.println(child + "   " + child.getChildren());
        }
    }
}
