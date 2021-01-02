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

import java.io.ByteArrayInputStream;
import java.util.jar.Attributes.Name;

import javax.lang.model.SourceVersion;

import bee.Task;
import bee.api.Command;
import bee.api.Library;
import bee.api.Scope;
import bee.util.Inputs;
import kiss.I;
import kiss.Signal;
import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.ClassWriter;
import net.bytebuddy.jar.asm.Opcodes;
import psychopath.Directory;
import psychopath.File;
import psychopath.Folder;
import psychopath.Locator;
import psychopath.Option;

public class Jar extends Task {

    /**
     * <p>
     * Package main classes and other resources.
     * </p>
     */
    @Command(value = "Package main classes and other resources.", defaults = true)
    public void source() {
        require(Compile::source);

        pack("main classes", I.signal(project.getClasses()), project
                .locateJar(), project.getJavaSourceVersion().compareTo(project.getJavaClassVersion()) > 0);
        pack("main sources", project.getSourceSet(), project.locateSourceJar(), false);
    }

    /**
     * <p>
     * Package test classes and other resources.
     * </p>
     */
    @Command("Package test classes and other resources.")
    public void test() {
        require(Compile::test);

        File classes = project.getOutput().file(project.getProduct() + "-" + project.getVersion() + "-tests.jar");
        File sources = project.getOutput().file(project.getProduct() + "-" + project.getVersion() + "-tests-sources.jar");

        pack("test classes", I.signal(project.getTestClasses()), classes, false);
        pack("test sources", project.getTestSourceSet(), sources, false);
    }

    /**
     * <p>
     * Package project classes and other resources.
     * </p>
     */
    @Command("Package project classes and other resources.")
    public void project() {
        require(Compile::project);

        File classes = project.getOutput().file(project.getProduct() + "-" + project.getVersion() + "-projects.jar");
        File sources = project.getOutput().file(project.getProduct() + "-" + project.getVersion() + "-projects-sources.jar");

        pack("project classes", I.signal(project.getProjectClasses()), classes, false);
        pack("project sources", project.getProjectSourceSet(), sources, false);
    }

    /**
     * <p>
     * Package documentations and other resources.
     * </p>
     */
    @Command("Package main documentations and other resources.")
    public void document() {
        Directory output = require(Doc::javadoc);

        pack("javadoc", I.signal(output), project.locateJavadocJar(), false);
    }

    /**
     * <p>
     * Packing.
     * </p>
     * 
     * @param type
     * @param input
     * @param output
     */
    private void pack(String type, Signal<Directory> input, File output, boolean modifyVersion) {
        ui.talk("Build ", type, " jar: ", output);

        if (modifyVersion) {
            String oldVersion = Inputs.normalize(project.getJavaSourceVersion());
            String newVersion = Inputs.normalize(project.getJavaClassVersion());
            ui.talk("Downgrade class version from ", oldVersion, " to ", newVersion, ".");

            input = input.map(dir -> {
                Directory modified = Locator.temporaryDirectory();

                dir.walkFile("**.class").to(file -> {
                    File modifiedFile = modified.file(dir.relativize(file));
                    ClassReader classReader = new ClassReader(file.bytes());
                    ClassWriter writer = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);
                    ClassVisitor modification = new Modify(project.getJavaClassVersion(), writer);
                    classReader.accept(modification, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
                    modifiedFile.write(new ByteArrayInputStream(writer.toByteArray()));
                });

                return modified;
            });
        }

        Locator.folder().add(input, Option::strip).packTo(output);
    }

    /**
     * <p>
     * Package main classes and other resources.
     * </p>
     */
    @Command("Package all main classes and resources with dependencies.")
    public void merge() {
        require(Jar::source);

        // create manifest
        File manifest = Locator.temporaryFile("MANIFEST.MF").text(
                /* Manifest contents */
                Name.MANIFEST_VERSION + ": 1.0", // version must be first
                Name.MAIN_CLASS + ": " + require(FindMain::main) // detect main class
        );

        File output = project.locateJar();
        ui.talk("Build merged classes jar: ", output);

        Folder folder = Locator.folder();
        folder.add(project.getClasses(), o -> o.strip());
        folder.add(manifest, o -> o.allocateIn("META-INF"));

        for (Library library : project.getDependency(Scope.Runtime)) {
            folder.add(library.getLocalJar().asArchive());
        }
        folder.packTo(output);
    }

    /**
     * 
     */
    private static class Modify extends ClassVisitor {

        private final int version;

        public Modify(SourceVersion ver, ClassVisitor classVisitor) {
            super(Opcodes.ASM8, classVisitor);
            switch (ver) {
            case RELEASE_1:
                version = Opcodes.V1_1;
                break;
            case RELEASE_2:
                version = Opcodes.V1_2;
                break;
            case RELEASE_3:
                version = Opcodes.V1_3;
                break;
            case RELEASE_4:
                version = Opcodes.V1_4;
                break;
            case RELEASE_5:
                version = Opcodes.V1_5;
                break;
            case RELEASE_6:
                version = Opcodes.V1_6;
                break;
            case RELEASE_7:
                version = Opcodes.V1_7;
                break;
            case RELEASE_8:
                version = Opcodes.V1_8;
                break;
            case RELEASE_9:
                version = Opcodes.V9;
                break;
            case RELEASE_10:
                version = Opcodes.V10;
                break;
            case RELEASE_11:
                version = Opcodes.V11;
                break;
            case RELEASE_12:
                version = Opcodes.V12;
                break;
            case RELEASE_13:
                version = Opcodes.V13;
                break;
            case RELEASE_14:
                version = Opcodes.V14;
                break;
            default:
                version = Opcodes.V15;
                break;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(this.version, access, name, signature, superName, interfaces);
        }
    }
}