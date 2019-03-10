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

import bee.Bee;
import bee.Task;
import bee.api.Command;
import bee.api.Scope;
import bee.util.JavaCompiler;
import kiss.Signal;
import psychopath.Directory;

/**
 * @version 2015/06/22 16:47:46
 */
public class Compile extends Task {

    /**
     * <p>
     * Compile main sources and copy other resources.
     * </p>
     */
    @Command(value = "Compile main sources and copy other resources.", defaults = true)
    public void source() {
        compile("main", project.getSourceSet(), project.getClasses());
    }

    /**
     * <p>
     * Compile test sources and copy other resources.
     * </p>
     */
    @Command("Compile test sources and copy other resources.")
    public void test() {
        compile("test", project.getTestSourceSet(), project.getTestClasses());
    }

    /**
     * <p>
     * Compile project sources and copy other resources.
     * </p>
     */
    @Command("Compile project sources and copy other resources.")
    public void project() {
        compile("project", project.getProjectSourceSet(), project.getProjectClasses());
    }

    /**
     * <p>
     * Helper method to compile sources and other resources.
     * </p>
     * 
     * @param type A source type.
     * @param input A source locations.
     * @param output A output location.
     */
    private void compile(String type, Signal<Directory> input, Directory output) {
        ui.talk("Copying ", type, " resources to ", output);
        input.to(dir -> {
            dir.copyTo(output, o -> o.glob("**", "!**.java").strip());
        });

        ui.talk("Compiling ", type, " sources to ", output);
        JavaCompiler compiler = new JavaCompiler(ui);
        compiler.addClassPath(project.getClasses());
        compiler.addClassPath(project.getDependency(Scope.Test, Scope.Compile));
        compiler.addSourceDirectory(input);
        compiler.setOutput(output);
        compiler.setNoWarn();
        compiler.compile();

        // load project related classes
        Bee.load(project.getClasses());
    }
}
