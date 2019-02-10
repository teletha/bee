/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import java.nio.file.Files;
import java.util.Set;

import bee.Bee;
import bee.api.Command;
import bee.api.Library;
import bee.api.Project;
import bee.api.Scope;
import bee.api.Task;
import kiss.I;
import kiss.XML;
import psychopath.Directory;
import psychopath.File;

/**
 * @version 2016/11/30 11:50:50
 */
public class IntelliJ extends Task implements IDESupport {

    /**
     * <p>
     * Create idea's project file.
     * </p>
     */
    @Override
    @Command("Generate configuration files for IntelliJ IDEA.")
    public void execute() {
        createModule(project.getSources(), project.getClasses(), Scope.Compile);
        createModule(project.getTestSources(), project.getTestClasses(), Scope.Test);
        createModule(project.getProjectSources(), project.getProjectClasses(), Scope.System);

        ui.talk("Create IDEA configuration files.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exist(Project project) {
        return Files.isReadable(project.getRoot().asJavaPath().resolve(".idea/modules.xml"));
    }

    /**
     * <p>
     * Create module configuration.
     * </p>
     * 
     * @param directory A configuration file.
     * @param output A class output directory.
     * @param scope A curretn scope.
     */
    private void createModule(Directory directory, Directory output, Scope scope) {
        XML doc = I.xml("module").attr("type", "JAVA_MODULE").attr("version", 4);
        XML component = doc.child("component").attr("name", "NewModuleRootManager").attr("inherit-compiler-output", false);
        component.child("output").attr("url", output);
        component.child("output-test").attr("url", project.getTestClasses());

        XML content = component.child("content").attr("url", "file://$MODULE_DIR$");
        content.child("sourceFolder").attr("url", "file://$MODULE_DIR$/java").attr("isTestSource", scope == Scope.Test);
        component.child("orderEntry").attr("type", "inheritedJdk");
        component.child("orderEntry").attr("type", "sourceFolder").attr("forTests", false);

        // Dependency Libraries
        library(project.getDependency(scope), component, scope);

        switch (scope) {
        case Test: // For Test Module
            component.child("orderEntry").attr("type", "module").attr("module-name", project.getSources().name());
            break;

        case System: // For Project Module
            if (!project.equals(Bee.TOOL)) {
                library(project.getLibrary(Bee.API.getGroup(), Bee.API.getProduct(), Bee.API.getVersion()), component, Scope.System);
            }
            break;
        }

        // write file
        makeFile(directory.file(directory.name() + ".iml"), doc);
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
            File jar = library.getLocalJar();
            File source = library.getLocalSourceJar();

            if (jar.isPresent()) {
                XML entry = root.child("orderEntry").attr("type", "module-library");

                if (scope == Scope.Test) {
                    entry.attr("scope", "TEST");
                }

                XML lib = entry.child("library").attr("name", library.toString());
                lib.child("CLASSES").child("root").attr("url", "jar://" + jar + "!/");

                if (source.isPresent()) {
                    lib.child("JAVADOC");
                    lib.child("SOURCES").child("root").attr("url", "jar://" + source + "!/");
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "IntelliJ";
    }
}
