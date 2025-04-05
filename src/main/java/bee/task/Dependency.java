/*
 * Copyright (C) 2025 The BEE Development Team
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

/**
 * Provides tasks for analyzing and displaying project dependencies.
 * This includes showing the dependency tree and analyzing required Java modules.
 */
public interface Dependency extends Task {

    /**
     * Displays the project's dependency graph in a tree format.
     * This command visualizes the direct and transitive dependencies of the project,
     * showing how libraries depend on each other.
     * This is the default command for the `dependency` task.
     */
    @Command(value = "Display the project dependency tree.", defaults = true)
    default void tree() {
        show(0, I.make(Repository.class).buildDependencyGraph(project()));
    }

    /**
     * Analyzes and displays the Java modules required by the project and its runtime dependencies.
     * Uses the `jdeps` tool to analyze bytecode and also considers `require-module` entries
     * in the `MANIFEST.MF` of dependency JARs.
     * Requires the main sources to be compiled first.
     *
     * @return A list of required module names.
     */
    @Command("Analyze and display required Java modules using jdeps.")
    default List<String> module() {
        require(Compile::source);

        Set<String> modules = new TreeSet();
        List<String> command = I.list("jdeps", "-q", "--print-module-deps", "--ignore-missing-deps", "--multi-release", "base");
        command.add(project().getClasses().path());
        for (Library library : project().getDependency(Scope.Runtime)) {
            command.add(library.getLocalJar().path());

            // Scrutinize the require-module listed in MANIFEST.MF of each Jar file. Note that
            // multiple modules are possible.
            try (JarFile file = new JarFile(library.getLocalJar().asJavaFile())) {
                Manifest manifest = file.getManifest();
                if (manifest != null) {
                    String requires = manifest.getMainAttributes().getValue("Require-Module");
                    if (requires != null) {
                        for (String require : requires.split("[ ,]+")) {
                            modules.add(require.trim());
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore errors reading manifest (e.g., invalid JAR)
                ui().debug("Could not read manifest for " + library.getLocalJar().name() + ": " + e.getMessage());
            }
        }
        modules.addAll(I.list(Process.with().read(command).split(",")));
        ui().info("Analyze dependency modules.", modules);
        return new ArrayList(modules);
    }

    /**
     * Recursively displays a dependency node and its children in a tree format.
     *
     * @param depth The current depth in the dependency tree (for indentation).
     * @param node The dependency node to display.
     */
    private void show(int depth, DependencyNode node) {
        Artifact artifact = node.getArtifact();
        StringBuilder name = new StringBuilder("\t".repeat(depth)).append(artifact.getGroupId())
                .append("  :  ")
                .append(artifact.getArtifactId())
                .append("  :  ");
        if (artifact.getClassifier().length() != 0) name.append(artifact.getClassifier()).append("  :  ");
        name.append(artifact.getVersion());

        ui().info(name);

        List<DependencyNode> children = node.getChildren();
        children.sort(Comparator.comparing(o -> o.getArtifact().getArtifactId()));

        for (DependencyNode child : children) {
            show(depth + 1, child);
        }
    }
}