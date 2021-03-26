/*
 * Copyright (C) 2021 Nameless Production Committee
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
import psychopath.Locator;

public class Compile extends Task {

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
        ui.talk("Copying ", type, " resources to ", output);
        input.to(dir -> {
            dir.observeCopyingTo(output, o -> o.glob("**", "!**.java").strip()).skipError().to();
        });

        ui.talk("Compiling ", type, " sources to ", output);
        JavaCompiler compiler = new JavaCompiler(ui);
        compiler.addClassPath(output);
        compiler.addClassPath(project.getClasses());
        compiler.addClassPath(project.getDependency(Scope.Compile, Scope.Test));
        compiler.addSourceDirectory(input);
        compiler.setOutput(output);
        compiler.setNoWarn();
        compiler.compile();

        // load project related classes
        Bee.load(project.getClasses());
    }
}