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

import java.io.ByteArrayInputStream;
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
import bee.api.Command;
import bee.api.Library;
import bee.api.Scope;
import bee.util.Inputs;
import kiss.I;
import kiss.Signal;
import psychopath.Directory;
import psychopath.File;
import psychopath.Folder;
import psychopath.Locator;
import psychopath.Option;

public class Jar extends Task {

    /**
     * Determines whether or not the class file should contain the local variable name and parameter
     * name.
     */
    protected boolean removeDebugInfo = false;

    /**
     * Determines whether or not the class file should contain the source file name and line number.
     */
    protected boolean removeTraceInfo = false;

    /**
     * Configure how to handle your resources when creating project jar.
     */
    protected Function<Option, Option> packing = Function.identity();

    /**
     * Configure how to handle merged resources when merging dependent jars.
     */
    protected Function<Option, Option> merging = Function.identity();

    /**
     * Package main classes and other resources.
     */
    @Command(value = "Package main classes and other resources.", defaults = true)
    public void source() {
        require(Compile::source);

        Directory dir = project.getClasses();
        if (SourceVersion.latest().compareTo(project.getJavaClassVersion()) > 0 || removeTraceInfo || removeDebugInfo) {
            dir = modify(dir);
        }

        pack("main classe", I.signal(dir), project.locateJar(), packing);
        pack("main source", project.getSourceSet(), project.locateSourceJar(), null);
    }

    /**
     * Modify class files.
     * 
     * @param dir
     * @return
     */
    private Directory modify(Directory dir) {
        String oldVersion = Inputs.normalize(SourceVersion.latest());
        String newVersion = Inputs.normalize(project.getJavaClassVersion());
        if (!oldVersion.equals(newVersion)) {
            ui.info("Downgrade class version from ", oldVersion, " to ", newVersion, ".");
        }
        if (removeDebugInfo) {
            ui.info("Remove all debugger-related information (local variables and parameters) from the class file.");
        }
        if (removeTraceInfo) {
            ui.info("Remove all debugging-related information (source file name and line number) from the class file.");
        }

        Directory modified = Locator.temporaryDirectory();

        dir.walkFile().to(file -> {
            File modifiedFile = modified.file(dir.relativize(file));

            if (file.extension().equals("class")) {
                ClassReader classReader = new ClassReader(file.bytes());
                ClassWriter writer = new ClassWriter(classReader, 0);
                ClassVisitor modification = new Modify(project.getJavaClassVersion(), writer);
                classReader.accept(modification, 0);
                modifiedFile.writeFrom(new ByteArrayInputStream(writer.toByteArray()));
            } else {
                file.copyTo(modifiedFile);
            }
        });

        return modified;
    }

    /**
     * Package test classes and other resources.
     */
    @Command("Package test classes and other resources.")
    public void test() {
        require(Compile::test);

        File classes = project.getOutput().file(project.getProduct() + "-" + project.getVersion() + "-tests.jar");
        File sources = project.getOutput().file(project.getProduct() + "-" + project.getVersion() + "-tests-sources.jar");

        pack("test class", I.signal(project.getTestClasses()), classes, null);
        pack("test source", project.getTestSourceSet(), sources, null);
    }

    /**
     * Package project classes and other resources.
     */
    @Command("Package project classes and other resources.")
    public void project() {
        require(Compile::project);

        File classes = project.getOutput().file(project.getProduct() + "-" + project.getVersion() + "-projects.jar");
        File sources = project.getOutput().file(project.getProduct() + "-" + project.getVersion() + "-projects-sources.jar");

        pack("project class", I.signal(project.getProjectClasses()), classes, null);
        pack("project source", project.getProjectSourceSet(), sources, null);
    }

    /**
     * Package documentations and other resources.
     */
    @Command("Package main documentations and other resources.")
    public void document() {
        Directory output = require(Doc::javadoc);

        pack("javadoc", I.signal(output), project.locateJavadocJar(), null);
    }

    /**
     * Packing.
     * 
     * @param type
     * @param input
     * @param output
     */
    private void pack(String type, Signal<Directory> input, File output, Function<Option, Option> option) {
        input = input.skipNull();
        option = option == null ? Function.identity() : option;

        Locator.folder()
                .add(input, option.andThen(Option::strip))
                .trackPackingTo(output)
                .to(Inputs.observerFor(ui, output, "Packaging " + type + " files", "Build " + type + " jar"));
    }

    /**
     * Package main classes and other resources.
     */
    @Command("Package all main classes and resources with dependencies.")
    public void merge() {
        require(Jar::source);

        // create manifest
        File manifest = Locator.temporaryFile("MANIFEST.MF").text(
                /* Manifest contents */
                Name.MANIFEST_VERSION + ": 1.0", // version must be first
                Name.MAIN_CLASS + ": " + require(FindMain::main), // detect main class
                "Launcher-Agent-Class: " + require(FindMain::agentmain), // detect agent main class
                "Agent-Class: " + require(FindMain::agentmain), // detect agent main class
                "Premain-Class: " + require(FindMain::premain) // detect pre main class
        );

        File output = project.locateJar();
        File temp = Locator.temporaryFile();
        output.moveTo(temp);

        Folder folder = Locator.folder();
        folder.add(temp.asArchive());
        folder.add(manifest, o -> o.allocateIn("META-INF"));

        for (Library library : project.getDependency(Scope.Runtime)) {
            folder.add(library.getLocalJar().asArchive(), merging);
        }
        folder.trackPackingTo(output).to(Inputs.observerFor(ui, output, "Merging class files", "Build merged classes jar"));
    }

    /**
     * 
     */
    private class Modify extends ClassVisitor {

        private final int version;

        public Modify(SourceVersion ver, ClassVisitor classVisitor) {
            super(Opcodes.ASM9, classVisitor);

            // ignore RELEASE_1
            version = 44 + Integer.parseInt(ver.name().substring(ver.name().indexOf('_') + 1));
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
            return new Minify(super.visitMethod(access, name, descriptor, signature, exceptions));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void visitSource(String source, String debug) {
            if (!removeTraceInfo) {
                super.visitSource(source, debug);
            }
        }
    }

    private class Minify extends MethodVisitor {

        /**
         * @param methodVisitor
         */
        public Minify(MethodVisitor methodVisitor) {
            super(Opcodes.ASM9, methodVisitor);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void visitParameter(String name, int access) {
            if (!removeDebugInfo) {
                super.visitParameter(name, access);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
            if (!removeDebugInfo) {
                super.visitLocalVariable(name, descriptor, signature, start, end, index);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String descriptor, boolean visible) {
            if (!removeDebugInfo) {
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
            if (!removeTraceInfo) {
                super.visitLineNumber(line, start);
            }
        }
    }
}