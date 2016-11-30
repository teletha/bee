/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import bee.Bee;
import bee.api.Command;
import bee.api.Library;
import bee.api.Scope;
import bee.api.Task;
import kiss.I;
import kiss.XML;

/**
 * @version 2016/11/30 11:50:50
 */
public class Idea extends Task {

    /**
     * <p>
     * Create idea's project file.
     * </p>
     */
    @Command("Generate configuration files for IntelliJ IDEA.")
    public void idea() {
        createModule(project.getSources(), Scope.Compile);
        createModule(project.getTestSources(), Scope.Test);
        createModule(project.getProjectSources(), Scope.System);

        ui.talk("Create IDEA configuration files.");
    }

    /**
     * <p>
     * Create module configuration.
     * </p>
     * 
     * @param file A configuration file.
     * @param scope A curretn scope.
     */
    private void createModule(Path file, Scope scope) {
        XML doc = I.xml("module").attr("type", "JAVA_MODULE").attr("version", 4);
        XML component = doc.child("component").attr("name", "NewModuleRootManager").attr("inherit-compiler-output", true);
        XML content = component.child("content").attr("url", "file://$MODULE_DIR$");
        content.child("sourceFolder").attr("url", "file://$MODULE_DIR$/java").attr("isTestSource", scope == Scope.Test);
        component.child("orderEntry").attr("type", "inheritedJdk");
        component.child("orderEntry").attr("type", "sourceFolder").attr("forTests", false);

        // Dependency Libraries
        library(project.getDependency(scope), component, scope);

        switch (scope) {
        case Test: // For Test Module
            component.child("orderEntry").attr("type", "module").attr("module-name", project.getSources().getFileName());
            break;

        case System: // For Project Module
            if (!project.equals(Bee.TOOL)) {
                library(project.getLibrary(Bee.API.getGroup(), Bee.API.getProduct(), Bee.API.getVersion()), component, Scope.System);
            }
            break;
        }

        // write file
        makeFile(file.resolve(file.getFileName() + ".iml"), doc);
    }

    /**
     * <p>
     * Write library configuration.
     * </p>
     * 
     * @param library A dependency library.
     * @param root A xml root.
     * @param scope A library scope.
     */
    private void library(Set<Library> libraries, XML root, Scope scope) {
        libraries.forEach(library -> {
            Path jar = library.getJar();
            Path source = library.getSourceJar();

            if (Files.exists(jar)) {
                XML entry = root.child("orderEntry").attr("type", "module-library");

                if (scope == Scope.Test) {
                    entry.attr("scope", "TEST");
                }

                XML lib = entry.child("library").attr("name", library.toString());
                lib.child("CLASSES").child("root").attr("url", "jar://" + jar + "!/");

                if (Files.exists(source)) {
                    lib.child("JAVADOC");
                    lib.child("SOURCES").child("root").attr("url", "jar://" + source + "!/");
                }
            }
        });
    }
}
