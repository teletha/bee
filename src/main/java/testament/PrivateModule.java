/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package testament;

import static java.nio.file.FileVisitResult.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import kiss.I;

/**
 * @version 2011/03/22 8:51:40
 */
public class PrivateModule extends ReusableRule {

    /** The actual private module. */
    public final Path path = I.locateTemporary();

    /** The original package name. */
    private final String originalPackage;

    /** The overridden package name. */
    private final String overriddenPackage;

    /** The private class filter. */
    private final PrivateClassStrategy strategy;

    /** Flag for creating jar. */
    private boolean createJar;

    /** The class loader of private module. */
    private ClassLoader loader;

    /**
     * <p>
     * Create private module with package name which is related to test class name.
     * </p>
     */
    public PrivateModule() {
        this(false, false);
    }

    /**
     * <p>
     * Create private module with package name which is related to test class name.
     * </p>
     */
    public PrivateModule(boolean renamePackage, boolean createJar) {
        this.createJar = createJar;

        // compute packaging structure
        originalPackage = testcase.getPackage().getName().replace('.', '/');
        overriddenPackage = renamePackage ? testcase.getSimpleName().toLowerCase() : originalPackage;

        strategy = new PrivateClassStrategy() {

            /**
             * {@inheritDoc}
             */
            @Override
            protected boolean accept(String fqcn) {
                return fqcn.startsWith(testcase.getName().replace('.', '/').concat("$"));
            }
        };
    }

    /**
     * <p>
     * Create private module with package name which is related to test class name.
     * </p>
     */
    public PrivateModule(String relativePath) {
        this(relativePath, false, false);
    }

    /**
     * <p>
     * Create private module with package name which is related to test class name.
     * </p>
     */
    public PrivateModule(String relativePath, boolean renamePackage, boolean createJar) {
        this.createJar = createJar;

        // normalize
        relativePath = relativePath.replace(File.separatorChar, '/');

        // compute packaging structure
        int index = relativePath.lastIndexOf('/');
        originalPackage = testcase.getPackage().getName().replace('.', '/') + "/" + relativePath;
        overriddenPackage = renamePackage ? index == -1 ? relativePath : relativePath.substring(index + 1)
                : originalPackage;

        strategy = new PrivateClassStrategy() {

            /**
             * {@inheritDoc}
             */
            @Override
            protected boolean accept(String fqcn) {
                return fqcn.startsWith(originalPackage);
            }
        };
    }

    /**
     * <p>
     * load this private module.
     * </p>
     */
    public void load() {
        loader = I.load(path);
    }

    /**
     * <p>
     * Unload this private module.
     * </p>
     */
    public void unload() {
        I.unload(path);
    }

    /**
     * <p>
     * Convert to the class associated with this private module.
     * </p>
     * 
     * @param clazz
     * @return A converted fuly qualified class name.
     */
    public Class convert(Class clazz) {
        try {
            return loader.loadClass(clazz.getName()
                    .replace(originalPackage.replace('/', '.'), overriddenPackage.replace('/', '.')));
        } catch (ClassNotFoundException e) {
            throw I.quiet(e);
        }
    }

    /**
     * @see testament.ReusableRule#beforeClass()
     */
    @Override
    protected void beforeClass() throws Exception {
        // copy class file with type conversion
        if (!createJar) {
            copy(testcaseRoot.resolve(originalPackage), path.resolve(overriddenPackage));
        } else {
            // create temporary
            Path temporary = I.locateTemporary();

            // copy class files with conversion
            copy(testcaseRoot.resolve(originalPackage), temporary.resolve(overriddenPackage));

            // create jar packer
            Archiver archiver = new Archiver(temporary, path);

            // scan all class files and pack it
            I.walk(temporary, archiver);

            // close stream properly
            archiver.output.close();
        }
    }

    /**
     * @see testament.ReusableRule#before(java.lang.reflect.Method)
     */
    @Override
    protected void before(Method method) throws Exception {
        load();
    }

    /**
     * @see testament.ReusableRule#after(java.lang.reflect.Method)
     */
    @Override
    protected void after(Method method) {
        unload();
    }

    /**
     * <p>
     * Copy class file with type conversion.
     * </p>
     * 
     * @param source A source directory.
     * @param target A target directory.
     */
    private void copy(Path source, Path target) {
        try {
            Files.createDirectories(target);

            for (Path path : Files.newDirectoryStream(source, strategy)) {
                Path dist = target.resolve(path.getFileName());

                if (Files.isDirectory(path)) {
                    copy(path, dist);
                } else {

                    if (!path.getFileName().toString().endsWith("class")) {
                        I.copy(source, dist);
                    } else {
                        // setup
                        ClassWriter writer = new ClassWriter(0);

                        // convert class file
                        new ClassReader(Files.newInputStream(path)).accept(new ClassConverter(writer), 0);

                        // write new class file
                        OutputStream stream = Files.newOutputStream(dist);
                        stream.write(writer.toByteArray());
                        stream.close();
                    }

                }
            }
        } catch (IOException e) {
            I.quiet(e);
        }
    }

    /**
     * <p>
     * Helper method to conver package name.
     * </p>
     * 
     * @param name A class name.
     * @return A converted name.
     */
    private String convert(String name) {
        return strategy.accept(name) ? name.replace(originalPackage, overriddenPackage) : name;
    }

    /**
     * <p>
     * Helper method to conver package name.
     * </p>
     * 
     * @param name A class name.
     * @return A converted name.
     */
    private String[] convert(String[] names) {
        if (names != null) {
            for (int i = 0; i < names.length; i++) {
                names[i] = convert(names[i]);
            }
        }

        return names;
    }

    /**
     * @version 2010/02/04 0:28:39
     */
    private final class ClassConverter extends ClassVisitor {

        /**
         * @param visiter
         */
        public ClassConverter(ClassVisitor visiter) {
            super(Opcodes.ASM4, visiter);
        }

        /**
         * @see org.objectweb.asm.ClassAdapter#visit(int, int, java.lang.String, java.lang.String,
         *      java.lang.String, java.lang.String[])
         */
        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, convert(name), signature, convert(superName), convert(interfaces));
        }

        /**
         * @see org.objectweb.asm.ClassAdapter#visitMethod(int, java.lang.String, java.lang.String,
         *      java.lang.String, java.lang.String[])
         */
        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            return new MethodConverter(super.visitMethod(access, convert(name), convert(desc), signature, convert(exceptions)));
        }
    }

    /**
     * @version 2010/02/04 0:28:33
     */
    private final class MethodConverter extends MethodVisitor {

        /**
         * @param paramMethodVisitor
         */
        public MethodConverter(MethodVisitor paramMethodVisitor) {
            super(Opcodes.ASM4, paramMethodVisitor);
        }

        /**
         * @see org.objectweb.asm.MethodAdapter#visitMethodInsn(int, java.lang.String,
         *      java.lang.String, java.lang.String)
         */
        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc) {
            super.visitMethodInsn(opcode, convert(owner), convert(name), convert(desc));
        }

        /**
         * @see org.objectweb.asm.MethodAdapter#visitFieldInsn(int, java.lang.String,
         *      java.lang.String, java.lang.String)
         */
        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            super.visitFieldInsn(opcode, convert(owner), convert(name), convert(desc));
        }
    }

    /**
     * @version 2010/10/14 1:16:59
     */
    private abstract class PrivateClassStrategy implements Filter<Path> {

        /** The pre-conputed path length. */
        private int prefix = testcaseRoot.toString().length() + 1;

        /**
         * @see java.nio.file.DirectoryStream.Filter#accept(java.lang.Object)
         */
        public boolean accept(Path path) throws IOException {
            return accept(path.toString().substring(prefix).replace(File.separatorChar, '/'));
        }

        /**
         * <p>
         * Determinate whether the given class name or path is acceptable or not.
         * </p>
         * 
         * @param fqcn Internal Form of Fully Qualified Class Name (JVMS 4.2).
         * @return A result.
         */
        protected abstract boolean accept(String fqcn);
    }

    /**
     * @version 2011/03/21 14:41:38
     */
    private static class Archiver extends SimpleFileVisitor<Path> {

        /** The actual jar output. */
        private final ZipOutputStream output;

        /** The base path. */
        private final Path base;

        /**
         * @param out
         * @param base
         */
        private Archiver(Path base, Path destination) throws IOException {
            this.output = new ZipOutputStream(Files.newOutputStream(destination));
            this.base = base;
        }

        /**
         * @see java.nio.file.FileVisitor#visitFile(java.lang.Object,
         *      java.nio.file.attribute.BasicFileAttributes)
         */
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            ZipEntry entry = new ZipEntry(base.relativize(file).toString().replace(File.separatorChar, '/'));
            entry.setSize(attrs.size());
            entry.setTime(attrs.lastModifiedTime().toMillis());
            output.putNextEntry(entry);

            // copy data
            InputStream input = Files.newInputStream(file);

            try {
                I.copy(input, output, false);
            } finally {
                I.quiet(input);
            }
            output.closeEntry();

            // API definition
            return CONTINUE;
        }
    }
}
