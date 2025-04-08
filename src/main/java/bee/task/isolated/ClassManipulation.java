/*
 * Copyright (C) 2025 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task.isolated;

import static bee.TaskOperations.*;

import java.io.ByteArrayInputStream;

import javax.lang.model.SourceVersion;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;

import bee.UserInterface;
import bee.api.Project;
import bee.task.Jar;
import bee.task.Jar.Config;
import bee.util.Inputs;
import psychopath.Directory;
import psychopath.File;
import psychopath.Locator;

/**
 * An ASM {@link ClassVisitor} implementation designed to modify class files.
 * <p>
 * This visitor performs the following modifications:
 * <ul>
 * <li>Downgrades the class file version to match a specified target Java version.</li>
 * <li>Optionally removes debug information (local variable names, parameter names) from
 * methods.</li>
 * <li>Optionally removes trace information (source file name, line numbers) from methods and the
 * class file.</li>
 * </ul>
 */
public class ClassManipulation extends ClassVisitor {

    public static Directory modify() {
        Project project = project();
        UserInterface ui = ui();
        Config conf = config(Jar.class);
        Directory modified = Locator.temporaryDirectory();

        String oldVersion = Inputs.normalize(SourceVersion.latest());
        String newVersion = Inputs.normalize(project.getJavaRequiredVersion());
        if (!oldVersion.equals(newVersion)) {
            ui.info("Downgrading class version from Java ", oldVersion, " to Java ", newVersion, ".");
        }
        if (conf.removeDebugInfo) {
            ui.info("Removing debug information (local variables, parameters) from class files.");
        }
        if (conf.removeTraceInfo) {
            ui.info("Removing trace information (source file name, line numbers) from class files.");
        }

        project.getClasses().walkFileWithBase().to(x -> {
            File modifiedFile = modified.file(x.ⅰ.relativize(x.ⅱ));

            if (x.ⅱ.extension().equals("class")) {
                ClassReader classReader = new ClassReader(x.ⅱ.bytes());
                ClassWriter writer = new ClassWriter(classReader, 0);
                ClassVisitor modification = new ClassManipulation(project.getJavaRequiredVersion(), writer, conf);
                classReader.accept(modification, 0);
                modifiedFile.writeFrom(new ByteArrayInputStream(writer.toByteArray()));
            } else {
                x.ⅱ.copyTo(modifiedFile);
            }
        });

        return modified;
    }

    /** Target major bytecode version calculated from SourceVersion. */
    private final int version;

    /** Configuration flags for removal options. */
    private final Config conf;

    /**
     * Creates a class modification visitor.
     *
     * @param targetVersion The target {@link SourceVersion} to which the class file version
     *            should
     *            be downgraded (if higher).
     * @param classVisitor The next {@link ClassVisitor} in the chain (usually a
     *            {@link ClassWriter}).
     * @param conf Configuration object containing {@code removeDebugInfo} and
     *            {@code removeTraceInfo} flags.
     */
    private ClassManipulation(SourceVersion targetVersion, ClassVisitor classVisitor, Config conf) {
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
        return new MethodMinify(super.visitMethod(access, name, descriptor, signature, exceptions), conf);
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

    /**
     * An ASM {@link MethodVisitor} implementation focused on removing debug and trace
     * information from a single method's bytecode based on the provided configuration.
     */
    private static class MethodMinify extends MethodVisitor {

        /** Configuration flags for removal options. */
        private final Jar.Config conf;

        /**
         * Creates a method visitor for minification.
         *
         * @param methodVisitor The next {@link MethodVisitor} in the chain.
         * @param conf Configuration object containing {@code removeDebugInfo} and
         *            {@code removeTraceInfo} flags.
         */
        private MethodMinify(MethodVisitor methodVisitor, Config conf) {
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
}