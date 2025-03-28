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

public interface Jar extends Task<Jar.Config> {

    /**
     * Package main classes and other resources.
     */
    @Command(value = "Package main classes and other resources.", defaults = true)
    default void source() {
        require(Compile::source);

        Config conf = config();
        Directory dir = TaskOperations.project().getClasses();
        if (SourceVersion.latest()
                .compareTo(TaskOperations.project().getJavaRequiredVersion()) > 0 || conf.removeTraceInfo || conf.removeDebugInfo) {
            dir = modify(dir);
        }

        pack("main classe", I.signal(dir), TaskOperations.project().locateJar(), conf.packing);
        pack("main source", TaskOperations.project().getSourceSet(), TaskOperations.project().locateSourceJar(), null);
    }

    /**
     * Modify class files.
     * 
     * @param dir
     * @return
     */
    private Directory modify(Directory dir) {
        String oldVersion = Inputs.normalize(SourceVersion.latest());
        String newVersion = Inputs.normalize(TaskOperations.project().getJavaRequiredVersion());
        if (!oldVersion.equals(newVersion)) {
            ui().info("Downgrade class version from ", oldVersion, " to ", newVersion, ".");
        }

        Config conf = config();
        if (conf.removeDebugInfo) {
            ui().info("Remove all debugger-related information (local variables and parameters) from the class file.");
        }
        if (conf.removeTraceInfo) {
            ui().info("Remove all debugging-related information (source file name and line number) from the class file.");
        }

        Directory modified = Locator.temporaryDirectory();

        dir.walkFile().to(file -> {
            File modifiedFile = modified.file(dir.relativize(file));

            if (file.extension().equals("class")) {
                ClassReader classReader = new ClassReader(file.bytes());
                ClassWriter writer = new ClassWriter(classReader, 0);
                ClassVisitor modification = new Modify(TaskOperations.project().getJavaRequiredVersion(), writer, config());
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
    default void test() {
        require(Compile::test);

        File classes = TaskOperations.project()
                .getOutput()
                .file(TaskOperations.project().getProduct() + "-" + TaskOperations.project().getVersion() + "-tests.jar");
        File sources = TaskOperations.project()
                .getOutput()
                .file(TaskOperations.project().getProduct() + "-" + TaskOperations.project().getVersion() + "-tests-sources.jar");

        pack("test class", I.signal(TaskOperations.project().getTestClasses()), classes, null);
        pack("test source", TaskOperations.project().getTestSourceSet(), sources, null);
    }

    /**
     * Package project classes and other resources.
     */
    @Command("Package project classes and other resources.")
    default void project() {
        require(Compile::project);

        File classes = TaskOperations.project()
                .getOutput()
                .file(TaskOperations.project().getProduct() + "-" + TaskOperations.project().getVersion() + "-projects.jar");
        File sources = TaskOperations.project()
                .getOutput()
                .file(TaskOperations.project().getProduct() + "-" + TaskOperations.project().getVersion() + "-projects-sources.jar");

        pack("project class", I.signal(TaskOperations.project().getProjectClasses()), classes, null);
        pack("project source", TaskOperations.project().getProjectSourceSet(), sources, null);
    }

    /**
     * Package documentations and other resources.
     */
    @Command("Package main documentations and other resources.")
    default void document() {
        Directory output = require(Doc::javadoc);

        pack("javadoc", I.signal(output), TaskOperations.project().locateJavadocJar(), null);
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
                .to(Inputs.observerFor(ui(), output, "Packaging " + type + " files", "Build " + type + " jar"));
    }

    /**
     * Package main classes and other resources.
     */
    @Command("Package all main classes and resources with dependencies.")
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
     * 
     */
    class Modify extends ClassVisitor {

        private final int version;

        private final Config conf;

        public Modify(SourceVersion ver, ClassVisitor classVisitor, Config conf) {
            super(Opcodes.ASM9, classVisitor);

            this.conf = conf;

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

    class Minify extends MethodVisitor {

        private final Config conf;

        /**
         * @param methodVisitor
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

    public static class Config {
        /**
         * Determines whether or not the class file should contain the local variable name and
         * parameter name.
         */
        public boolean removeDebugInfo = false;

        /**
         * Determines whether or not the class file should contain the source file name and line
         * number.
         */
        public boolean removeTraceInfo = false;

        /**
         * Configure how to handle your resources when creating project jar.
         */
        public Function<Option, Option> packing = Function.identity();

        /**
         * Configure how to handle merged resources when merging dependent jars.
         */
        public Function<Option, Option> merging = Function.identity();
    }
}