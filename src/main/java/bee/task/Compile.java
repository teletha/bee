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

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

import bee.api.Command;
import bee.api.Scope;
import bee.api.Task;
import bee.util.JavaCompiler;
import bee.util.PathSet;
import kiss.I;

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
        compile("main", project.getSources(), project.getClasses().base);
    }

    /**
     * <p>
     * Compile test sources and copy other resources.
     * </p>
     */
    @Command("Compile test sources and copy other resources.")
    public void test() {
        compile("test", project.getTestSources(), project.getTestClasses());
    }

    /**
     * <p>
     * Compile project sources and copy other resources.
     * </p>
     */
    @Command("Compile project sources and copy other resources.")
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
        compiler.addClassPath(project.getClasses().base);
        compiler.addClassPath(project.getDependency(Scope.Test));
        compiler.addSourceDirectory(input);
        compiler.setOutput(output);
        compiler.setNoWarn();
        compiler.compile();

        // load project related classes
        try {
            Method add = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            add.setAccessible(true);

            add.invoke(project.getClass().getClassLoader(), output.toUri().toURL());
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }
}
