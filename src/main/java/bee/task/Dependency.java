/*
 * Copyright (C) 2024 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import java.util.Comparator;
import java.util.List;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;

import bee.Task;
import bee.api.Command;
import bee.api.Repository;
import kiss.I;

public class Dependency extends Task {

    @Command("Display the dependency tree.")
    public void tree() {
        show(0, I.make(Repository.class).buildDependencyGraph(project));
    }

    /**
     * Write out the dependency.
     * 
     * @param depth
     * @param node
     */
    private void show(int depth, DependencyNode node) {
        Artifact artifact = node.getArtifact();
        StringBuilder name = new StringBuilder("\t".repeat(depth)).append(artifact.getGroupId())
                .append("  :  ")
                .append(artifact.getArtifactId())
                .append("  :  ");
        if (artifact.getClassifier().length() != 0) name.append(artifact.getClassifier()).append("  :  ");
        name.append(artifact.getVersion());

        ui.info(name);

        List<DependencyNode> children = node.getChildren();
        children.sort(Comparator.comparing(o -> o.getArtifact().getArtifactId()));

        for (DependencyNode child : children) {
            show(depth + 1, child);
        }
    }
}