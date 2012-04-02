/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import java.nio.file.Path;

import bee.compiler.JavaCompiler;
import bee.definition.Scope;
import bee.util.PathSet;

/**
 * @version 2012/04/02 1:09:24
 */
public class Compile extends Task {

    /**
     * <p>
     * Compile main sources and copy other resources.
     * </p>
     */
    @Command(defaults = true, description = "Compile main sources and copy other resources.")
    public void source() {
        compile("main", project.getSources(), project.getClasses());
    }

    /**
     * <p>
     * Compile test sources and copy other resources.
     * </p>
     */
    @Command(description = "Compile test sources and copy other resources.")
    public void test() {
        compile("test", project.getTestSources(), project.getTestClasses());
    }

    /**
     * <p>
     * Compile project sources and copy other resources.
     * </p>
     */
    @Command(description = "Compile project sources and copy other resources.")
    public void project() {
        compile("project", project.getProjectSources(), project.getProjectClasses());
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
    private void compile(String type, PathSet input, Path output) {
        ui.talk("Copying ", type, " resources to ", output);
        input.copyTo(output, "**", "!**.java");

        ui.talk("Compiling ", type, " sources to ", output);
        JavaCompiler compiler = new JavaCompiler();
        compiler.addClassPath(project.getClasses());
        compiler.addClassPath(project.getDependency(Scope.Test));
        compiler.addSourceDirectory(input);
        compiler.setOutput(output);
        compiler.setNoWarn();
        compiler.compile();
    }
}
