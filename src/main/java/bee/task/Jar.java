/*
 * Copyright (C) 2018 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import java.nio.file.Path;

import bee.api.Command;
import bee.api.Library;
import bee.api.Scope;
import bee.api.Task;
import bee.util.JarArchiver;
import bee.util.PathPattern;
import bee.util.PathSet;

/**
 * @version 2017/01/16 14:40:35
 */
public class Jar extends Task {

    /**
     * <p>
     * Package main classes and other resources.
     * </p>
     */
    @Command(value = "Package main classes and other resources.", defaults = true)
    public void source() {
        require(Compile.class).source();

        pack("main classes", new PathSet(project.getClasses()), project.locateJar());
        pack("main sources", project.getSourceSet(), project.locateSourceJar());
    }

    /**
     * <p>
     * Package test classes and other resources.
     * </p>
     */
    @Command("Package test classes and other resources.")
    public void test() {
        require(Compile.class).test();

        Path classes = project.getOutput().resolve(project.getProduct() + "-" + project.getVersion() + "-tests.jar");
        Path sources = project.getOutput().resolve(project.getProduct() + "-" + project.getVersion() + "-tests-sources.jar");

        pack("test classes", new PathSet(project.getTestClasses()), classes);
        pack("test sources", project.getTestSourceSet(), sources);
    }

    /**
     * <p>
     * Package project classes and other resources.
     * </p>
     */
    @Command("Package project classes and other resources.")
    public void project() {
        require(Compile.class).project();

        Path classes = project.getOutput().resolve(project.getProduct() + "-" + project.getVersion() + "-projects.jar");
        Path sources = project.getOutput().resolve(project.getProduct() + "-" + project.getVersion() + "-projects-sources.jar");

        pack("project classes", new PathSet(project.getProjectClasses()), classes);
        pack("project sources", project.getProjectSourceSet(), sources);
    }

    /**
     * <p>
     * Package documentations and other resources.
     * </p>
     */
    @Command("Package main documentations and other resources.")
    public void document() {
        Doc doc = require(Doc.class);
        doc.javadoc();

        pack("javadoc", new PathSet(doc.output), project.locateJavadocJar());
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
    private void pack(String type, PathSet input, Path output) {
        ui.talk("Build ", type, " jar: ", output);

        JarArchiver archiver = new JarArchiver();
        for (PathPattern path : input) {
            archiver.add(path);
        }
        archiver.pack(output);
    }

    /**
     * <p>
     * Package main classes and other resources.
     * </p>
     */
    @Command("Package all main classes and resources with dependencies.")
    public void merge() {
        require(Compile.class).source();
        String main = require(FindMain.class).main();

        Path output = project.locateJar();
        ui.talk("Build merged classes jar: ", output);

        JarArchiver archiver = new JarArchiver();
        archiver.setMainClass(main);
        archiver.add(project.getClasses());

        for (Library library : project.getDependency(Scope.Runtime)) {
            archiver.add(library.getLocalJar());
        }
        archiver.pack(output);
    }
}
