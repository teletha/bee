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

import java.util.jar.Attributes.Name;

import bee.Task;
import bee.api.Command;
import bee.api.Library;
import bee.api.Scope;
import kiss.I;
import kiss.Signal;
import psychopath.Directory;
import psychopath.File;
import psychopath.Folder;
import psychopath.Locator;
import psychopath.Option;

public class Jar extends Task {

    /**
     * <p>
     * Package main classes and other resources.
     * </p>
     */
    @Command(value = "Package main classes and other resources.", defaults = true)
    public void source() {
        require(Compile::source);

        pack("main classes", I.signal(project.getClasses()), project.locateJar());
        pack("main sources", project.getSourceSet(), project.locateSourceJar());
    }

    /**
     * <p>
     * Package test classes and other resources.
     * </p>
     */
    @Command("Package test classes and other resources.")
    public void test() {
        require(Compile::test);

        File classes = project.getOutput().file(project.getProduct() + "-" + project.getVersion() + "-tests.jar");
        File sources = project.getOutput().file(project.getProduct() + "-" + project.getVersion() + "-tests-sources.jar");

        pack("test classes", I.signal(project.getTestClasses()), classes);
        pack("test sources", project.getTestSourceSet(), sources);
    }

    /**
     * <p>
     * Package project classes and other resources.
     * </p>
     */
    @Command("Package project classes and other resources.")
    public void project() {
        require(Compile::project);

        File classes = project.getOutput().file(project.getProduct() + "-" + project.getVersion() + "-projects.jar");
        File sources = project.getOutput().file(project.getProduct() + "-" + project.getVersion() + "-projects-sources.jar");

        pack("project classes", I.signal(project.getProjectClasses()), classes);
        pack("project sources", project.getProjectSourceSet(), sources);
    }

    /**
     * <p>
     * Package documentations and other resources.
     * </p>
     */
    @Command("Package main documentations and other resources.")
    public void document() {
        Directory output = require(Doc::javadoc);

        pack("javadoc", I.signal(output), project.locateJavadocJar());
    }

    /**
     * <p>
     * Packing.
     * </p>
     * 
     * @param type
     * @param input
     * @param output
     */
    private void pack(String type, Signal<Directory> input, File output) {
        ui.talk("Build ", type, " jar: ", output);

        Locator.folder().add(input, Option::strip).packTo(output);
    }

    /**
     * <p>
     * Package main classes and other resources.
     * </p>
     */
    @Command("Package all main classes and resources with dependencies.")
    public void merge() {
        // create manifest
        File manifest = Locator.temporaryFile("MANIFEST.MF").text(
                /* Manifest contents */
                Name.MANIFEST_VERSION + ": 1.0", // version must be first
                Name.MAIN_CLASS + ": " + require(FindMain::main) // detect main class
        );

        File output = project.locateJar();
        ui.talk("Build merged classes jar: ", output);

        Folder folder = Locator.folder();
        folder.add(project.getClasses(), o -> o.strip());
        folder.add(manifest, o -> o.allocateIn("META-INF"));

        for (Library library : project.getDependency(Scope.Runtime)) {
            folder.add(library.getLocalJar().asArchive());
        }
        folder.packTo(output);
    }
}
