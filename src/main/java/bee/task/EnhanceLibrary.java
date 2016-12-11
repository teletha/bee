/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import static jdk.internal.org.objectweb.asm.Opcodes.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import bee.Platform;
import bee.api.Scope;
import bee.api.Task;
import bee.util.PathSet;
import bee.util.Paths;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;
import kiss.Events;
import kiss.I;
import kiss.Table;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * @version 20 import kiss.Table;16/12/10 13:15:20
 */
public class EnhanceLibrary extends Task {

    /**
     * <p>
     * Enhance external class.
     * </p>
     * 
     * @return A path to enhanced class library.
     */
    @SneakyThrows
    public List<Path> enhance(List<Path> jars) {
        for (Entry<Path, List<ExtensionDefinition>> entry : searchExtensionLibrary().entrySet()) {
            // copy original archive
            Path original = entry.getKey();

            if (jars.remove(original)) {
                Path enhanced = manageLocalLibrary(original);
                jars.add(enhanced);

                Path jar = jar(enhanced);
                Table<Class, ExtensionDefinition> table = Events.from(entry.getValue()).toTable(ExtensionDefinition::target);

                for (Entry<Class, List<ExtensionDefinition>> definitions : table.entrySet()) {
                    Class clazz = definitions.getKey();
                    Path classFile = jar.resolve(clazz.getName().replace('.', '/') + ".class");

                    // start writing byte code
                    ClassReader reader = new ClassReader(Files.newInputStream(classFile));
                    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                    reader.accept(new Enhancer(writer, definitions.getValue()), ClassReader.EXPAND_FRAMES);

                    // write new byte code
                    Files.copy(new ByteArrayInputStream(writer.toByteArray()), classFile, StandardCopyOption.REPLACE_EXISTING);

                    ui.talk("Enhance " + clazz.getName() + ".");
                }
            }
        }
        return jars;
    }

    /**
     * @version 2016/12/11 14:07:30
     */
    private static class Enhancer extends ClassVisitor {

        /** The definitions. */
        private final List<ExtensionDefinition> definitions;

        /**
         * Class file enhancer.
         * 
         * @param writer An actual writer.
         * @param definitions A list of extension definitions.
         */
        private Enhancer(ClassWriter writer, List<ExtensionDefinition> definitions) {
            super(Opcodes.ASM5, writer);

            this.definitions = definitions;

            String v = "";
            System.out.println(v.isNotEmpty());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void visitEnd() {
            // add extension methods
            for (ExtensionDefinition def : definitions) {
                String className = Type.getInternalName(def.extensionClass);
                Type callee = Type.getMethodType(Type.getMethodDescriptor(def.method));
                Type[] param = Type.getArgumentTypes(def.method);
                Type caller = Type.getMethodType(callee.getReturnType(), Arrays.asList(param)
                        .subList(1, param.length)
                        .toArray(new Type[param.length - 1]));
                System.out.println(callee.getDescriptor() + "  " + className + "  " + def.method.getName());
                System.out.println(caller.getDescriptor() + "  " + def.method.getName());
                MethodVisitor mv = visitMethod(ACC_PUBLIC, def.method.getName(), caller.getDescriptor(), null, null);
                mv.visitCode();
                mv.visitVarInsn(ALOAD, 0); // load 'this'
                mv.visitMethodInsn(INVOKESTATIC, className, def.method.getName(), callee.getDescriptor(), false);
                mv.visitInsn(caller.getOpcode(IRETURN));
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
            super.visitEnd();
        }
    }

    /**
     * @param value
     * @return
     */
    @ExtensionMethod
    public static boolean isNotEmpty(String value) {
        return !value.isEmpty();
    }

    public boolean isNotEmpty() {
        return EnhanceLibrary.isNotEmpty((String) (Object) this);
    }

    /**
     * @param value
     * @return
     */
    @ExtensionMethod
    public static boolean isBlank(String value) {
        return value.length() == 0;
    }

    /** The timestamp format. */
    private static final DateTimeFormatter timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * <p>
     * Create the new local library path and delete all old local libraries.
     * </p>
     * 
     * @param original
     * @return
     */
    @SneakyThrows
    private Path manageLocalLibrary(Path original) {
        String name = Paths.getName(original);
        Path root = Paths.createDirectory(project.getOutput().resolve("local-library"));

        // delete old libraries at later
        for (Path path : I.walk(root, name + "-*.jar")) {
            path.toFile().deleteOnExit();
        }

        // create new library with time stamp
        Path created = root.resolve(name + "-" + LocalDateTime.now().format(timestamp) + ".jar");

        // copy actual jar file
        I.copy(original, created);

        // API definition
        return created;
    }

    /**
     * <p>
     * Search all extension libraries.
     * </p>
     */
    private Table<Path, ExtensionDefinition> searchExtensionLibrary() {
        return Events.from(project.getDependency(Scope.Compile))
                .map(lib -> lib.getJar())
                .map(this::jar)
                .startWith(project.getSources().resolve("resources"))
                .map(path -> path.resolve("META-INF/services/" + EnhanceLibrary.class.getName()))
                .take(Files::exists)
                .flatIterable(Files::readAllLines)
                .map(ExtensionDefinition::of)
                .toTable(ExtensionDefinition::archive);
    }

    /**
     * <p>
     * Resolve jar file.
     * </p>
     * 
     * @param path
     * @return
     * @throws IOException
     */
    private Path jar(Path path) throws IOException {
        return FileSystems.newFileSystem(path, ClassLoader.getSystemClassLoader()).getPath("/");
    }

    /**
     * @version 2016/12/11 1:30:03
     */
    @Value
    @Accessors(fluent = true)
    private static class ExtensionDefinition {

        Class target;

        Path archive;

        Class extensionClass;

        Method method;

        /**
         * @param line
         * @return
         * @throws ClassNotFoundException
         */
        @SneakyThrows
        static ExtensionDefinition of(String line) throws ClassNotFoundException {
            String[] values = line.split(" ");
            Class extensionClass = Class.forName(values[0]);
            String[] params = values[2].split(",");
            Class[] paramTypes = new Class[params.length];

            for (int i = 0; i < paramTypes.length; i++) {
                paramTypes[i] = Class.forName(params[i]);
            }
            Method method = extensionClass.getMethod(values[1], paramTypes);
            Path archive = I.locate(paramTypes[0]);

            return new ExtensionDefinition(paramTypes[0], archive == null ? Platform.JavaRuntime : archive, extensionClass, method);
        }
    }

    /**
     * <p>
     * Copy JRE related jars to project local directory.
     * </p>
     */
    void copyJRE() {
        Path local = project.getOutput().resolve("jre");
        collectJRE().copyTo(local);
    }

    /**
     * <p>
     * Collect JRE related jars.
     * </p>
     * 
     * @return
     */
    private PathSet collectJRE() {
        return new PathSet(I.walk(Platform.JavaRuntime
                .getParent(), "**.jar", "!plugin.jar", "!management-agent.jar", "!jfxswt.jar", "!java*", "!security/*", "!deploy.jar"));
    }

    private void collectEnhancers() {
        // try {
        // for (Library library : project.getDependency(Scope.Runtime)) {
        // Path file = FileSystems.newFileSystem(library.getJar(),
        // ClassLoader.getSystemClassLoader())
        // .getPath("/")
        // .resolve("META-INF/services/bee.enhancer.ExtensionMethod");
        // if (Files.exists(file)) {
        // libraries.add(library.getJar());
        // }
        // }
        // } catch (IOException e) {
        // throw I.quiet(e);
        // }
    }

    /**
     * @version 2016/12/10 13:20:14
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ExtensionMethod {
    }
}
