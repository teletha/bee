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

import bee.Task;
import bee.TaskOperations;
import bee.api.Command;
import bee.api.Comment;
import bee.api.Scope;
import bee.util.JavaCompiler;
import kiss.Signal;
import psychopath.Directory;
import psychopath.Locator;

public interface Compile extends Task<Compile.Config> {

    /**
     * Compile main sources and copy other resources.
     */
    @Command(value = "Compile main sources and copy other resources.", defaults = true)
    default void source() {
        compile("main", TaskOperations.project().getSourceSet(), TaskOperations.project().getClasses());
    }

    /**
     * Compile test sources and copy other resources.
     */
    @Command("Compile test sources and copy other resources.")
    default void test() {
        require(Compile::source);

        compile("test", TaskOperations.project().getTestSourceSet(), TaskOperations.project().getTestClasses());
    }

    /**
     * Compile project sources and copy other resources.
     */
    @Command("Compile project sources and copy other resources.")
    default void project() {
        compile("project", TaskOperations.project().getProjectSourceSet(), TaskOperations.project().getProjectClasses());
    }

    /**
     * Validate all sources which are compilable or not.
     */
    @Command(value = "Compile main sources and copy other resources.", defaults = true)
    default void check() {
        Directory dir = Locator.temporaryDirectory();
        compile("main", TaskOperations.project().getSourceSet(), dir);
        compile("test", TaskOperations.project().getTestSourceSet(), dir);
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
        ui().info("Copying ", type, " resources to ", output);
        input.to(dir -> {
            dir.observeCopyingTo(output, o -> o.glob("**", "!**.java").strip()).skipError().to();
        });

        ui().info("Compiling ", type, " sources to ", output);

        JavaCompiler.with(ui())
                .addClassPath(output)
                .addClassPath(TaskOperations.project().getClasses())
                .addClassPath(TaskOperations.project().getDependency(Scope.Compile, Scope.Test, Scope.Annotation))
                .addSourceDirectory(input)
                .setVersion(TaskOperations.project().getJavaRequiredVersion())
                .setOutput(output)
                .setNoWarn()
                .setEncoding(TaskOperations.project().getEncoding())
                .setEclipseCompiler(config().useECJ)
                .compile();

        // load project related classes
        // BeeLoader.load(TaskOperations.project().getClasses());
    }

    /**
     * Configuration for {@link Compile} task.
     */
    public static class Config {

        @Comment("Force to use the eclipse compiler for Java.")
        public boolean useECJ = false;
    }
}