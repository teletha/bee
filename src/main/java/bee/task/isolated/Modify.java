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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import bee.TaskOperations;
import bee.task.Jar.Config;
import bee.util.Inputs;
import psychopath.Directory;
import psychopath.File;

/**
 * ASM ClassVisitor implementation to modify class version and delegate to method/source
 * visitors.
 */
public class Modify extends ClassVisitor {

    public static void modify(Directory original, Directory modified, Config conf) {
        String oldVersion = Inputs.normalize(SourceVersion.latest());
        String newVersion = Inputs.normalize(TaskOperations.project().getJavaRequiredVersion());
        if (!oldVersion.equals(newVersion)) {
            ui().info("Downgrading class version from Java ", oldVersion, " to Java ", newVersion, ".");
        }

        if (conf.removeDebugInfo) {
            ui().info("Removing debug information (local variables, parameters) from class files.");
        }
        if (conf.removeTraceInfo) {
            ui().info("Removing trace information (source file name, line numbers) from class files.");
        }

        original.walkFile().to(file -> {
            File modifiedFile = modified.file(original.relativize(file));

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
    }

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