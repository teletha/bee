/*
 * Copyright (C) 2020 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import java.io.ByteArrayInputStream;

import javax.lang.model.SourceVersion;

import bee.Bee;
import bee.Task;
import bee.api.Command;
import bee.api.Scope;
import bee.util.Inputs;
import bee.util.JavaCompiler;
import kiss.Signal;
import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.ClassWriter;
import net.bytebuddy.jar.asm.Opcodes;
import psychopath.Directory;

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
        compile("main", project.getSourceSet(), project.getClasses());
    }

    /**
     * <p>
     * Compile test sources and copy other resources.
     * </p>
     */
    @Command("Compile test sources and copy other resources.")
    public void test() {
        compile("test", project.getTestSourceSet(), project.getTestClasses());
    }

    /**
     * <p>
     * Compile project sources and copy other resources.
     * </p>
     */
    @Command("Compile project sources and copy other resources.")
    public void project() {
        compile("project", project.getProjectSourceSet(), project.getProjectClasses());
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
    private void compile(String type, Signal<Directory> input, Directory output) {
        ui.talk("Copying ", type, " resources to ", output);
        input.to(dir -> {
            dir.observeCopyingTo(output, o -> o.glob("**", "!**.java").strip()).skipError().to();
        });

        ui.talk("Compiling ", type, " sources to ", output);
        JavaCompiler compiler = new JavaCompiler(ui);
        compiler.addClassPath(project.getClasses());
        compiler.addClassPath(project.getDependency(Scope.Test, Scope.Compile));
        compiler.addSourceDirectory(input);
        compiler.setOutput(output);
        compiler.setNoWarn();
        compiler.compile();

        if (project.getJavaSourceVersion().compareTo(project.getJavaClassVersion()) > 0) {
            String oldVersion = Inputs.normalize(project.getJavaSourceVersion());
            String newVersion = Inputs.normalize(project.getJavaClassVersion());
            ui.talk("Downgrade class version from ", oldVersion, " to ", newVersion, ".");

            project.getClasses().walkFile("**.class").to(file -> {
                ClassReader classReader = new ClassReader(file.bytes());
                ClassWriter writer = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);
                ClassVisitor modification = new Modify(project.getJavaClassVersion(), writer);
                classReader.accept(modification, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
                file.write(new ByteArrayInputStream(writer.toByteArray()));
            });
        }

        // load project related classes
        Bee.load(project.getClasses());
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
            default:
                version = Opcodes.V14;
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