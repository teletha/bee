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

/**
 * Compiles project source code, test code, and project-specific definition code.
 * This task also handles copying associated resource files to the output directories.
 */
public interface Compile extends Task<Compile.Config> {

    /**
     * Compiles main source files found in the source directory set (e.g., `src/main/java`)
     * and copies associated resources (e.g., files in `src/main/resources`) to the main classes
     * directory.
     * This is the default command for the `compile` task.
     */
    @Command(value = "Compile main sources and resources.", defaults = true)
    default void source() {
        compile("main", TaskOperations.project().getSourceSet(), TaskOperations.project().getClasses());
    }

    /**
     * Compiles test source files found in the test source directory set (e.g., `src/test/java`)
     * and copies associated resources (e.g., files in `src/test/resources`) to the test classes
     * directory.
     * Requires the main sources to be compiled first.
     */
    @Command("Compile test sources and resources.")
    default void test() {
        require(Compile::source);

        compile("test", TaskOperations.project().getTestSourceSet(), TaskOperations.project().getTestClasses());
    }

    /**
     * Compiles project definition source files (e.g., `src/project/java`)
     * and copies associated resources to the project classes directory.
     * This is typically used for compiling the Bee Project class itself.
     */
    @Command("Compile project definition sources and resources.")
    default void project() {
        compile("project", TaskOperations.project().getProjectSourceSet(), TaskOperations.project().getProjectClasses());
    }

    /**
     * Performs a validation compile run on main and test sources without producing final output,
     * primarily to check for compilation errors. Useful for quick syntax and type checking.
     * Temporary output directory is used and cleaned up afterwards.
     */
    @Command("Validate main and test sources for compilation errors.")
    default void check() {
        Directory temp = Locator.temporaryDirectory();
        try {
            ui().info("Checking main sources...");
            compile("main", TaskOperations.project().getSourceSet(), temp);
            ui().info("Checking test sources...");
            compile("test", TaskOperations.project().getTestSourceSet(), temp);
            ui().info("Source validation completed successfully.");
        } finally {
            temp.deleteOnExit(); // Ensure cleanup even if errors occur during compile
        }
    }

    /**
     * Internal helper method to perform the compilation and resource copying process.
     *
     * @param type A string identifying the type of sources being processed (e.g., "main", "test").
     *            Used for logging.
     * @param input A signal providing the source directories to process.
     * @param output The target directory for compiled classes and copied resources.
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
    }

    /**
     * Configuration settings for the {@link Compile} task.
     * These settings can be adjusted in the project definition file.
     */
    class Config {

        @Comment("If true, forces the use of the Eclipse Compiler for Java (ECJ) instead of the standard JDK compiler.")
        public boolean useECJ = false;
    }
}