/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.extension;

import static jdk.internal.org.objectweb.asm.Opcodes.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
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
import bee.UserInterface;
import bee.api.Project;
import bee.api.Scope;
import bee.util.Paths;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Label;
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
 * @version 2016/12/12 21:24:29
 */
public class JavaExtension {

    /** The timestamp format. */
    private static final DateTimeFormatter timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** The user interface. */
    private final UserInterface ui;

    /** The current project. */
    private final Project project;

    /** The extension definition repository. */
    private Table<Path, Definition> extensions;

    /**
     * @param ui
     * @param project
     */
    JavaExtension(UserInterface ui, Project project) {
        this.ui = ui;
        this.project = project;
        this.extensions = Events.from(project.getDependency(Scope.Compile))
                .map(lib -> lib.getJar())
                .map(this::jar)
                .startWith(project.getSources().resolve("resources"))
                .map(path -> path.resolve("META-INF/services/" + JavaExtension.class.getName()))
                .take(Files::exists)
                .flatIterable(I.quiet(Files::readAllLines))
                .map(Definition::of)
                .skipNull()
                .toTable(Definition::archive);
    }

    /**
     * <p>
     * Check whether some extension exist or not.
     * </p>
     * 
     * @return A result.
     */
    public boolean hasExtension() {
        return extensions.isEmpty() == false;
    }

    /**
     * <p>
     * Check whether JRE extension exist or not.
     * </p>
     * 
     * @return A result.
     */
    public boolean hasJREExtension() {
        return extensions.containsKey(Platform.JavaRuntime);
    }

    /**
     * <p>
     * List up all enhanced JRE jars.
     * </p>
     * 
     * @return A list of jars.
     */
    public List<Path> enhancedJRE() {
        return enhance(collectJRE());
    }

    /**
     * <p>
     * Enhance external class.
     * </p>
     * 
     * @return A path to enhanced class library.
     */
    @SneakyThrows
    public List<Path> enhance(List<Path> jars) {
        for (Entry<Path, List<Definition>> archives : extensions.entrySet()) {
            // copy original archive
            Path archive = archives.getKey();

            if (jars.remove(archive)) {
                Path enhanced = manageLocalLibrary(archive);
                jars.add(enhanced);

                Path jar = jar(enhanced);

                for (Entry<Class, List<Definition>> definitions : Events.from(archives.getValue()).toTable(Definition::target).entrySet()) {
                    Path classFile = jar.resolve(definitions.getKey().getName().replace('.', '/') + ".class");

                    // start writing byte code
                    ClassReader reader = new ClassReader(Files.newInputStream(classFile));
                    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                    reader.accept(new Enhancer(writer, definitions.getValue()), ClassReader.EXPAND_FRAMES);

                    // write new byte code
                    Files.copy(new ByteArrayInputStream(writer.toByteArray()), classFile, StandardCopyOption.REPLACE_EXISTING);
                }

                // cleanup temporary files
                I.delete(enhanced.getParent(), "zipfstmp*");

                ui.talk("Enhance " + archive.getFileName() + ".");
            }
        }
        return jars;
    }

    /**
     * <p>
     * Collect JRE related jars.
     * </p>
     * 
     * @return
     */
    private List<Path> collectJRE() {
        return I.walk(Platform.JavaRuntime
                .getParent(), "**.jar", "!plugin.jar", "!management-agent.jar", "!jfxswt.jar", "!java*", "!security/*", "!deploy.jar");
    }

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
            try {
                I.delete(path);
            } catch (Exception e) {
                // ignore
            }
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
     * Resolve jar file.
     * </p>
     * 
     * @param path
     * @return
     * @throws IOException
     */
    @SneakyThrows
    private Path jar(Path path) {
        return FileSystems.newFileSystem(path, ClassLoader.getSystemClassLoader()).getPath("/");
    }

    /**
     * @version 2016/12/11 14:07:30
     */
    private static class Enhancer extends ClassVisitor {

        /** The definitions. */
        private final List<Definition> definitions;

        /**
         * Class file enhancer.
         * 
         * @param writer An actual writer.
         * @param definitions A list of extension definitions.
         */
        private Enhancer(ClassWriter writer, List<Definition> definitions) {
            super(Opcodes.ASM5, writer);

            this.definitions = definitions;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void visitEnd() {
            // add extension methods
            for (Definition def : definitions) {
                String className = Type.getInternalName(def.extensionClass);
                Type callee = Type.getMethodType(Type.getMethodDescriptor(def.method));
                Class[] params = def.method.getParameterTypes();
                Type[] param = Type.getArgumentTypes(def.method);
                Type caller = Type.getMethodType(callee.getReturnType(), Arrays.asList(param)
                        .subList(1, param.length)
                        .toArray(new Type[param.length - 1]));

                int localIndex = params.length;

                MethodVisitor mv = visitMethod(ACC_PUBLIC, def.method.getName(), caller.getDescriptor(), null, null);
                mv.visitCode();
                Label l0 = new Label();
                Label l1 = new Label();
                Label l2 = new Label();
                mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");
                mv.visitLabel(l0);
                mv.visitIntInsn(BIPUSH, params.length);
                mv.visitTypeInsn(ANEWARRAY, "java/lang/Object"); // new Object[params.length]
                mv.visitInsn(DUP);
                mv.visitInsn(ICONST_0);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitInsn(AASTORE); // first parameter must be 'this'
                for (int i = 1; i < params.length; i++) {
                    mv.visitInsn(DUP); // copy array
                    mv.visitIntInsn(BIPUSH, i); // array index
                    mv.visitVarInsn(param[i].getOpcode(ILOAD), i); // load param
                    wrap(params[i], mv);
                    mv.visitInsn(AASTORE); // store param into array
                }
                mv.visitVarInsn(ASTORE, localIndex); // store params into local variable

                Label l3 = new Label();
                mv.visitLabel(l3);
                mv.visitIntInsn(BIPUSH, params.length);
                mv.visitTypeInsn(ANEWARRAY, "java/lang/String"); // new String[params.length]
                for (int i = 0; i < params.length; i++) {
                    mv.visitInsn(DUP); // copy array
                    mv.visitIntInsn(BIPUSH, i); // array index
                    mv.visitLdcInsn(params[i].getName()); // load param type
                    mv.visitInsn(AASTORE); // store param into array
                }
                mv.visitVarInsn(ASTORE, localIndex + 1); // store paramTypes into local variable

                Label l4 = new Label();
                mv.visitLabel(l4);
                mv.visitVarInsn(ALOAD, localIndex);
                mv.visitInsn(ARRAYLENGTH); // calculate param length
                mv.visitTypeInsn(ANEWARRAY, "java/lang/Class"); // new Class[params.length]
                mv.visitVarInsn(ASTORE, localIndex + 2); // store types into local variable

                // for loop to load parameter classes
                Label l5 = new Label();
                mv.visitLabel(l5);
                mv.visitInsn(ICONST_0);
                mv.visitVarInsn(ISTORE, localIndex + 3); // int i = 0;
                Label l6 = new Label();
                mv.visitLabel(l6);
                Label l7 = new Label();
                mv.visitJumpInsn(GOTO, l7);
                Label l8 = new Label();
                mv.visitLabel(l8);
                mv.visitVarInsn(ALOAD, localIndex + 2); // paramTypes
                mv.visitVarInsn(ILOAD, localIndex + 3); // i
                mv.visitVarInsn(ALOAD, localIndex + 1); // paramNames
                mv.visitVarInsn(ILOAD, localIndex + 3); // i
                mv.visitInsn(AALOAD); // paramNames[i]
                mv.visitInsn(ICONST_1); // true
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/ClassLoader", "getSystemClassLoader", "()Ljava/lang/ClassLoader;", false);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", false);
                mv.visitInsn(AASTORE);
                Label l9 = new Label();
                mv.visitLabel(l9);
                mv.visitIincInsn(localIndex + 3, 1); // increment i
                mv.visitLabel(l7);
                mv.visitVarInsn(ILOAD, localIndex + 3);
                mv.visitVarInsn(ALOAD, localIndex + 2);
                mv.visitInsn(ARRAYLENGTH);
                mv.visitJumpInsn(IF_ICMPLT, l8);

                // load extension class
                Label l10 = new Label();
                mv.visitLabel(l10);
                mv.visitLdcInsn(def.extensionClass.getName()); // class name
                mv.visitInsn(ICONST_1); // true
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/ClassLoader", "getSystemClassLoader", "()Ljava/lang/ClassLoader;", false);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", false);
                mv.visitVarInsn(ASTORE, localIndex + 3); // store extensionClass into local variable

                // load extension method
                Label l11 = new Label();
                mv.visitLabel(l11);
                mv.visitVarInsn(ALOAD, localIndex + 3); // load extension class
                mv.visitLdcInsn(def.method.getName()); // load method name
                mv.visitVarInsn(ALOAD, localIndex + 2); // load parameter types
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", false);

                // invoke extension method
                mv.visitInsn(ACONST_NULL);
                mv.visitVarInsn(ALOAD, 1); // load actual parameters
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);

                // check return type
                cast(def.method.getReturnType(), mv);

                // return
                mv.visitLabel(l1);
                mv.visitInsn(Type.getReturnType(def.method).getOpcode(IRETURN));

                // catch exception
                mv.visitLabel(l2);
                mv.visitFrame(Opcodes.F_FULL, 1, new Object[] {className}, 1, new Object[] {"java/lang/Exception"});
                mv.visitVarInsn(ASTORE, 1);
                Label l12 = new Label();
                mv.visitLabel(l12);
                mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
                mv.visitInsn(DUP);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/Throwable;)V", false);
                mv.visitInsn(ATHROW);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
            super.visitEnd();
        }

        /**
         * Helper method to write cast code. This cast mostly means down cast. (e.g. Object ->
         * String, Object -> int)
         *
         * @param clazz A class to cast.
         * @return A class type to be casted.
         */
        private Type cast(Class clazz, MethodVisitor mv) {
            Type type = Type.getType(clazz);

            if (clazz.isPrimitive()) {
                if (clazz != Void.TYPE) {
                    Type wrapper = Type.getType(I.wrap(clazz));
                    mv.visitTypeInsn(CHECKCAST, wrapper.getInternalName());
                    mv.visitMethodInsn(INVOKEVIRTUAL, wrapper
                            .getInternalName(), clazz.getName() + "Value", "()" + type.getDescriptor(), false);
                }
            } else {
                mv.visitTypeInsn(CHECKCAST, type.getInternalName());
            }

            // API definition
            return type;
        }

        /**
         * Helper method to write cast code. This cast mostly means up cast. (e.g. String -> Object,
         * int -> Integer)
         *
         * @param clazz A primitive class type to wrap.
         */
        private void wrap(Class clazz, MethodVisitor mv) {
            if (clazz.isPrimitive() && clazz != Void.TYPE) {
                Type wrapper = Type.getType(I.wrap(clazz));
                mv.visitMethodInsn(INVOKESTATIC, wrapper
                        .getInternalName(), "valueOf", "(" + Type.getType(clazz).getDescriptor() + ")" + wrapper.getDescriptor(), false);
            }
        }
    }

    /**
     * @version 2016/12/11 1:30:03
     */
    @Value
    @Accessors(fluent = true)
    private static class Definition {

        /** The target class to extend. */
        Class target;

        /** The archive of the target class. */
        Path archive;

        /** The extension class. */
        Class extensionClass;

        /** The extension method. */
        java.lang.reflect.Method method;

        /**
         * <p>
         * Parse definition.
         * </p>
         * 
         * @param line A definition.
         * @return A parsed definition.
         */
        static Definition of(String line) {
            try {
                String[] values = line.split(" ");
                Class extensionClass = Class.forName(values[0]);
                String[] params = values[2].split(",");
                Class[] paramTypes = new Class[params.length];

                for (int i = 0; i < paramTypes.length; i++) {
                    paramTypes[i] = Class.forName(params[i]);
                }
                java.lang.reflect.Method method = extensionClass.getMethod(values[1], paramTypes);
                Path archive = I.locate(paramTypes[0]);

                return new Definition(paramTypes[0], archive == null ? Platform.JavaRuntime : archive, extensionClass, method);
            } catch (Exception e) {
                return null;
            }
        }
    }

    /**
     * @version 2016/12/12 21:07:08
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Method {
    }

    public boolean isNotEmpty(int value, String name) {
        try {
            Object[] params = {this, value, name};
            String[] paramNames = {"java.lang.String", "int", "java.lang.String"};
            Class[] paramTypes = new Class[paramNames.length];

            for (int i = 0; i < paramTypes.length; i++) {
                paramTypes[i] = Class.forName(paramNames[i], true, ClassLoader.getSystemClassLoader());
            }
            System.out.println(Arrays.toString(paramTypes));

            Class clazz = Class.forName("bee.task.EnhanceLibrary", true, ClassLoader.getSystemClassLoader());
            return ((Boolean) clazz.getMethod("isNotEmpty", paramTypes).invoke(null, params)).booleanValue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // return EnhanceLibrary.isNotEmpty((String) (Object) this);
    }

    /**
     * @param value
     * @return
     */
    @Method
    public static boolean isNotEmpty(String value) {
        return !value.isEmpty();
    }

    /**
     * @param value
     * @return
     */
    @Method
    public static boolean isBlank(String value) {
        return value.length() == 0;
    }

    /**
     * @param value
     * @return
     */
    @Method
    public static boolean isColorName(String value) {
        return value.length() == 0;
    }
}
