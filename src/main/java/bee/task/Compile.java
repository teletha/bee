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

import bee.Task;
import bee.api.Command;
import bee.api.Scope;
import bee.util.JavaCompiler;
import kiss.Signal;
import psychopath.Directory;
import psychopath.Locator;

public class Compile extends Task {

    /** Force to use the eclipse compiler for Java. */
    public boolean useECJ = false;

    /**
     * Compile main sources and copy other resources.
     */
    @Command(value = "Compile main sources and copy other resources.", defaults = true)
    public void source() {
        compile("main", project.getSourceSet(), project.getClasses());
    }

    /**
     * Compile test sources and copy other resources.
     */
    @Command("Compile test sources and copy other resources.")
    public void test() {
        require(Compile::source);

        compile("test", project.getTestSourceSet(), project.getTestClasses());
    }

    /**
     * Compile project sources and copy other resources.
     */
    @Command("Compile project sources and copy other resources.")
    public void project() {
        compile("project", project.getProjectSourceSet(), project.getProjectClasses());
    }

    /**
     * Validate all sources which are compilable or not.
     */
    @Command(value = "Compile main sources and copy other resources.", defaults = true)
    public void check() {
        Directory dir = Locator.temporaryDirectory();
        compile("main", project.getSourceSet(), dir);
        compile("test", project.getTestSourceSet(), dir);
        dir.deleteOnExit();
    }

    /**
     * Helper method to compile sources and other resources.
     * 
     * @param type A source type.
     * @param input A source locations.
     * @param output A output location.
     */
    private void compile(String type, Signal<Directory> input, Directory output) {
        ui.info("Copying ", type, " resources to ", output);
        input.to(dir -> {
            dir.observeCopyingTo(output, o -> o.glob("**", "!**.java").strip()).skipError().to();
        });

        ui.info("Compiling ", type, " sources to ", output);

        JavaCompiler.with(ui)
                .addClassPath(output)
                .addClassPath(project.getClasses())
                .addClassPath(project.getDependency(Scope.Compile, Scope.Test, Scope.Annotation))
                .addSourceDirectory(input)
                .setVersion(project.getJavaRequiredVersion())
                .setOutput(output)
                .setNoWarn()
                .setEncoding(project.getEncoding())
                .setEclipseCompiler(useECJ)
                .compile();

        // load project related classes
        // BeeLoader.load(project.getClasses());
    }
}