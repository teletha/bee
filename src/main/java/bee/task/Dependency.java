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

import static bee.TaskOperations.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;

import bee.Task;
import bee.api.Command;
import bee.api.Library;
import bee.api.Repository;
import bee.api.Scope;
import bee.util.Process;
import kiss.I;

public class Dependency extends Task {

    @Command(value = "Display the dependency tree.", defaults = true)
    public void tree() {
        show(0, I.make(Repository.class).buildDependencyGraph(project));
    }

    @Command("Display all dependency modules.")
    public List<String> module() {
        require(Compile::source);

        Set<String> modules = new TreeSet();
        List<String> command = I.list("jdeps", "-q", "--print-module-deps", "--ignore-missing-deps", "--multi-release", "base");
        command.add(project.getClasses().path());
        for (Library library : project.getDependency(Scope.Runtime)) {
            command.add(library.getLocalJar().path());

            // Scrutinize the require-module listed in MANIFEST.MF of each Jar file. Note that
            // multiple modules are possible.
            try (JarFile file = new JarFile(library.getLocalJar().asJavaFile())) {
                Manifest manifest = file.getManifest();
                if (manifest != null) {
                    String requires = manifest.getMainAttributes().getValue("require-module");
                    if (requires != null) {
                        for (String require : requires.split("[ ,]+")) {
                            modules.add(require);
                        }
                    }
                }
            } catch (Exception e) {
                throw I.quiet(e);
            }
        }
        modules.addAll(I.list(Process.with().read(command).split(",")));
        ui.info("Analyze dependency modules.", modules);
        return new ArrayList(modules);
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