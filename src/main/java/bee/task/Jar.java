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

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.jar.Attributes.Name;

import javax.lang.model.SourceVersion;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;

import bee.Task;
import bee.TaskOperations;
import bee.api.Command;
import bee.api.Comment;
import bee.api.Library;
import bee.api.Project;
import bee.api.Scope;
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
    @Command(value = "Package main classes and resources into a JAR file. Also creates a source JAR.", defaults = true)
    default void source() {
        require(Compile::source); // Ensure main code is compiled

        Project project = TaskOperations.project();
        Config conf = config();
        Directory classesDir = project.getClasses();

        // Modify class files if Java version needs downgrade or if removal options are enabled
        boolean requiresModification = SourceVersion.latest()
                .compareTo(project.getJavaRequiredVersion()) > 0 || conf.removeTraceInfo || conf.removeDebugInfo;

        if (requiresModification) {
            classesDir = modify(classesDir); // Use modified classes for packaging
        }

        // Package classes and sources
        pack("main classes", I.signal(classesDir), project.locateJar(), conf.packing);
        pack("main sources", project.getSourceSet(), project.locateSourceJar(), null);
    }

    /**
     * Internal helper to modify class files (version downgrade, debug/trace info removal).
     * Creates modified class files in a temporary directory.
     *
     * @param originalDir The directory containing the original class files.
     * @return The temporary directory containing the modified class files.
     */
    private Directory modify(Directory dir) {
        String oldVersion = Inputs.normalize(SourceVersion.latest());
        String newVersion = Inputs.normalize(TaskOperations.project().getJavaRequiredVersion());
        if (!oldVersion.equals(newVersion)) {
            ui().info("Downgrading class version from Java ", oldVersion, " to Java ", newVersion, ".");
        }

        Config conf = config();
        if (conf.removeDebugInfo) {
            ui().info("Removing debug information (local variables, parameters) from class files.");
        }
        if (conf.removeTraceInfo) {
            ui().info("Removing trace information (source file name, line numbers) from class files.");
        }

        Directory modified = Locator.temporaryDirectory();

        dir.walkFile().to(file -> {
            File modifiedFile = modified.file(dir.relativize(file));

            if (file.extension().equals("class")) {
                ClassReader classReader = new ClassReader(file.bytes());
                ClassWriter writer = new ClassWriter(classReader, 0);
                ClassVisitor modification = new Modify(TaskOperations.project().getJavaRequiredVersion(), writer, conf);
                classReader.accept(modification, 0);
                modifiedFile.writeFrom(new ByteArrayInputStream(writer.toByteArray()));
            } else {
                file.copyTo(modifiedFile);
            }
        });

        return modified;
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
                .to(Inputs.observerFor(ui(), output, "Packaging " + type + " files", "Build " + type + " jar"));
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
        folder.trackPackingTo(output).to(Inputs.observerFor(ui(), output, "Merging class files", "Build merged classes jar"));
    }

    /**
     * ASM ClassVisitor implementation to modify class version and delegate to method/source
     * visitors.
     */
    class Modify extends ClassVisitor {

        private final int version;

        private final Config conf;

        /**
         * Creates a class modification visitor.
         * 
         * @param targetVersion The target Java source version.
         * @param classVisitor The next visitor in the chain.
         * @param conf Configuration for debug/trace info removal.
         */
        public Modify(SourceVersion targetVersion, ClassVisitor classVisitor, Config conf) {
            super(Opcodes.ASM9, classVisitor);

            this.conf = conf;

            // ignore RELEASE_1
            version = 44 + Integer.parseInt(targetVersion.name().substring(targetVersion.name().indexOf('_') + 1));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(this.version, access, name, signature, superName, interfaces);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            return new Minify(super.visitMethod(access, name, descriptor, signature, exceptions), conf);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void visitSource(String source, String debug) {
            if (!conf.removeTraceInfo) {
                super.visitSource(source, debug);
            }
        }
    }

    /**
     * ASM MethodVisitor implementation to remove debug and trace information.
     */
    class Minify extends MethodVisitor {

        private final Config conf;

        /**
         * Creates a method visitor for minification.
         * 
         * @param methodVisitor The next visitor in the chain.
         * @param conf Configuration for debug/trace info removal.
         */
        public Minify(MethodVisitor methodVisitor, Config conf) {
            super(Opcodes.ASM9, methodVisitor);
            this.conf = conf;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void visitParameter(String name, int access) {
            if (!conf.removeDebugInfo) {
                super.visitParameter(name, access);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
            if (!conf.removeDebugInfo) {
                super.visitLocalVariable(name, descriptor, signature, start, end, index);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String descriptor, boolean visible) {
            if (!conf.removeDebugInfo) {
                return super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, descriptor, visible);
            } else {
                return null;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void visitLineNumber(int line, Label start) {
            if (!conf.removeTraceInfo) {
                super.visitLineNumber(line, start);
            }
        }
    }

    /**
     * Configuration settings for the {@link Jar} task.
     */
    public static class Config {
        @Comment("Determines whether or not the class file should contain the local variable name and parameter name.")
        public boolean removeDebugInfo = false;

        @Comment("Determines whether or not the class file should contain the source file name and line number.")
        public boolean removeTraceInfo = false;

        @Comment("Configure how to handle your resources when creating project jar.")
        public Function<Option, Option> packing = Function.identity();

        @Comment("Configure how to handle merged resources when merging dependent jars.")
        public Function<Option, Option> merging = Function.identity();
    }
}