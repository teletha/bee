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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.jar.Attributes.Name;

import javax.lang.model.SourceVersion;

import bee.Isolation;
import bee.Task;
import bee.TaskOperations;
import bee.api.Command;
import bee.api.Comment;
import bee.api.Library;
import bee.api.Project;
import bee.api.Scope;
import bee.task.isolated.ClassManipulation;
import bee.util.Inputs;
import kiss.I;
import kiss.Signal;
import psychopath.Directory;
import psychopath.File;
import psychopath.Folder;
import psychopath.Locator;
import psychopath.Option;

/**
 * Handles the packaging of project artifacts (classes, sources, resources, documentation) into JAR
 * files.
 * Provides options for creating standard JARs, source JARs, test JARs, documentation JARs,
 * and executable "uber-jars" containing all dependencies.
 * Also includes functionality to modify class files (e.g., version downgrade, debug info removal).
 */
public interface Jar extends Task<Jar.Config> {

    /**
     * Packages the main compiled classes and resources into a standard JAR file.
     * Also creates a separate JAR containing the main source files and resources.
     * If configured, modifies class files (version downgrade, debug/trace info removal) before
     * packaging.
     * This is the default command for the `jar` task.
     */
    @SuppressWarnings("serial")
    @Command(value = "Package main classes and resources into a JAR file. Also creates a source JAR.", defaults = true)
    default void source() {
        require(Compile::source); // Ensure main code is compiled

        Config conf = config();
        Project project = TaskOperations.project();
        Directory classesDir = project.getClasses();

        // Modify class files if Java version needs downgrade or if removal options are enabled
        boolean requiresModify = SourceVersion.latest()
                .compareTo(project.getJavaRequiredVersion()) > 0 || conf.removeTraceInfo || conf.removeDebugInfo;

        if (requiresModify) {
            classesDir = new Isolation<Directory>("org.ow2.asm : asm") {
                @Override
                protected Directory bridge() {
                    return ClassManipulation.modify();
                }
            }.result();
        }

        // Package classes and sources
        pack("main classes", I.signal(classesDir), project.locateJar(), conf.packing);
        pack("main sources", project.getSourceSet(), project.locateSourceJar(), null);
    }

    /**
     * Packages the compiled test classes and resources into a test JAR file.
     * Also creates a separate JAR containing the test source files and resources.
     */
    @Command("Package test classes and resources into a JAR file. Also creates a test source JAR.")
    default void test() {
        require(Compile::test);

        Project project = TaskOperations.project();
        File classes = project.getOutput().file(project.getProduct() + "-" + project.getVersion() + "-tests.jar");
        File sources = project.getOutput().file(project.getProduct() + "-" + project.getVersion() + "-tests-sources.jar");

        pack("test class", I.signal(project.getTestClasses()), classes, null);
        pack("test source", project.getTestSourceSet(), sources, null);
    }

    /**
     * Packages the compiled project definition classes (e.g., the Project class itself) and
     * resources into a project JAR file.
     * Also creates a separate JAR containing the project source files and resources.
     */
    @Command("Package project definition classes and resources into a JAR file. Also creates a project source JAR.")
    default void project() {
        require(Compile::project);

        Project project = TaskOperations.project();
        File classes = project.getOutput().file(project.getProduct() + "-" + project.getVersion() + "-projects.jar");
        File sources = project.getOutput().file(project.getProduct() + "-" + project.getVersion() + "-projects-sources.jar");

        pack("project class", I.signal(project.getProjectClasses()), classes, null);
        pack("project source", project.getProjectSourceSet(), sources, null);
    }

    /**
     * Packages the generated Javadoc documentation into a documentation JAR file.
     */
    @Command("Package generated Javadoc into a JAR file.")
    default void document() {
        Directory output = require(Doc::javadoc);

        Project project = TaskOperations.project();
        pack("javadoc", I.signal(output), project.locateJavadocJar(), null);
    }

    /**
     * Internal helper method to pack files from input directories into an output JAR file.
     *
     * @param type A string describing the type of content being packed (for logging).
     * @param input A signal providing the input directories containing files to pack.
     * @param output The target JAR file to create.
     * @param option A function to configure packing options (e.g., file filtering, path stripping),
     *            can be null.
     */
    private void pack(String type, Signal<Directory> input, File output, Function<Option, Option> option) {
        input = input.skipNull();
        option = option == null ? Function.identity() : option;

        Locator.folder()
                .add(input, option.andThen(Option::strip))
                .trackPackingTo(output)
                .to(Inputs.progress(output, "Packaging " + type + " files", "Build " + type + " jar"));
    }

    /**
     * Creates a single executable JAR file (uber-jar or fat-jar) containing the main classes,
     * resources, and all runtime dependencies unpacked and merged together.
     * Automatically detects and sets the Main-Class and agent attributes in the JAR's manifest
     * file.
     */
    @Command("Create an executable JAR with all dependencies included (uber-jar).")
    default void merge() {
        require(Jar::source);

        // create manifest
        List<String> lines = new ArrayList();
        lines.add(Name.MANIFEST_VERSION + ": 1.0");
        require(FindMain::main).to(clazz -> lines.add(Name.MAIN_CLASS + ": " + clazz));
        require(FindMain::agentmain).to(clazz -> lines.add("Launcher-Agent-Class: " + clazz));
        require(FindMain::agentmain).to(clazz -> lines.add("Agent-Class: " + clazz));
        require(FindMain::premain).to(clazz -> lines.add("Premain-Class: " + clazz));

        File manifest = Locator.temporaryFile("MANIFEST.MF").text(lines);
        File output = TaskOperations.project().locateJar();
        File temp = Locator.temporaryFile();
        output.moveTo(temp);

        Folder folder = Locator.folder();
        folder.add(temp.asArchive());
        folder.add(manifest, o -> o.allocateIn("META-INF"));

        Config conf = config();
        for (Library library : TaskOperations.project().getDependency(Scope.Runtime)) {
            folder.add(library.getLocalJar().asArchive(), conf.merging);
        }
        folder.trackPackingTo(output).to(Inputs.progress(output, "Merging class files", "Build merged classes jar"));
    }

    /**
     * Configuration settings for the {@link Jar} task.
     */
    @SuppressWarnings("serial")
    public static class Config implements Serializable {
        @Comment("Determines whether or not the class file should contain the local variable name and parameter name.")
        public boolean removeDebugInfo = false;

        @Comment("Determines whether or not the class file should contain the source file name and line number.")
        public boolean removeTraceInfo = false;

        @Comment("Configure how to handle your resources when creating project jar.")
        public transient Function<Option, Option> packing = Function.identity();

        @Comment("Configure how to handle merged resources when merging dependent jars.")
        public transient Function<Option, Option> merging = Function.identity();
    }
}