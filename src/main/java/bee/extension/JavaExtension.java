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

import static java.nio.file.StandardCopyOption.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map.Entry;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import bee.Platform;
import bee.UserInterface;
import bee.api.Project;
import bee.util.Paths;
import filer.Filer;
import kiss.I;
import kiss.Table;

/**
 * @version 2016/12/13 16:07:08
 */
public class JavaExtension {

    /** The timestamp format. */
    private static final DateTimeFormatter timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** The user interface. */
    private final UserInterface ui;

    /** The current project. */
    private final Project project;

    /** The extension repository. */
    private Table<Path, JavaExtensionMethodDefinition> methods = new Table();

    /**
     * @param ui
     * @param project
     */
    JavaExtension(UserInterface ui, Project project) {
        this.ui = ui;
        this.project = project;

        this.methods = I.signal(I.findAs(Extension.class))
                .flatArray(clazz -> clazz.getMethods())
                .take(this::validateExtensionMethod)
                .map(JavaExtensionMethodDefinition::new)
                .toTable(methods, m -> m.archive);
    }

    /**
     * <p>
     * Check whether some extension exist or not.
     * </p>
     * 
     * @return A result.
     */
    public boolean hasExtension() {
        return methods.isEmpty() == false;
    }

    /**
     * <p>
     * Check whether JRE extension exist or not.
     * </p>
     * 
     * @return A result.
     */
    public boolean hasJREExtension() {
        return methods.containsKey(Platform.JavaRuntime);
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
    public List<Path> enhance(List<Path> jars) {
        try {
            for (Entry<Path, List<JavaExtensionMethodDefinition>> archives : methods.entrySet()) {
                // copy original archive
                Path archive = archives.getKey();

                if (jars.remove(archive)) {
                    Path enhanced = manageLocalLibrary(archive);
                    jars.add(enhanced);

                    try (FileSystem zip = jar(enhanced)) {
                        Table<Class, JavaExtensionMethodDefinition> defs = I.signal(archives.getValue()).toTable(def -> def.targetClass);

                        for (Entry<Class, List<JavaExtensionMethodDefinition>> definitions : defs.entrySet()) {
                            Class clazz = definitions.getKey();
                            Path classFile = zip.getPath(clazz.getName().replace('.', '/') + ".class");

                            // start writing byte code
                            byte[] bytes = enhanceMethodExtension(Files.readAllBytes(classFile), definitions.getValue());

                            // write new byte code
                            Files.copy(new ByteArrayInputStream(bytes), classFile, REPLACE_EXISTING);
                        }

                        // cleanup temporary files
                        // Filer.delete(enhanced.getParent(), "zipfstmp*");
                        ui.talk("Enhance " + archive + " to " + enhanced + ".");
                    }
                }
            }
        } catch (IOException e) {
            throw I.quiet(e);
        }
        return jars;
    }

    /**
     * <p>
     * Enhance class file with the specified definitions.
     * </p>
     * 
     * @param classFile The byte code of the extened class file.
     * @param definitions The definitions of extension method.
     * @return The extended byte code.
     */
    private byte[] enhanceMethodExtension(byte[] classFile, List<JavaExtensionMethodDefinition> definitions) {
        ClassReader reader = new ClassReader(classFile);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        reader.accept(new JavaExtensionMethodEnhancer(writer, definitions.get(0).targetClass, definitions), ClassReader.EXPAND_FRAMES);

        return writer.toByteArray();
    }

    /**
     * <p>
     * Collect JRE related jars.
     * </p>
     * 
     * @return
     */
    private List<Path> collectJRE() {
        return Filer.walk(Platform.JavaRuntime
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
    private Path manageLocalLibrary(Path original) {
        String name = Paths.getName(original);
        Path root = Paths.createDirectory(project.getOutput().resolve("local-library"));

        // delete old libraries at later
        for (Path path : Filer.walk(root, name + "-*.jar")) {
            try {
                Filer.delete(path);
            } catch (Exception e) {
                // ignore
            }
        }

        try {
            // create new library with time stamp
            Path local = root.resolve(name + "-" + LocalDateTime.now().format(timestamp) + ".jar");

            // copy actual jar file
            return Files.copy(original, local, COPY_ATTRIBUTES);
        } catch (IOException e) {
            throw I.quiet(e);
        }
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
    private FileSystem jar(Path path) {
        try {
            return FileSystems.newFileSystem(path, ClassLoader.getSystemClassLoader());
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * Validate method as extension.
     * </p>
     * 
     * @param method A method to validate.
     * @return A result.
     */
    private boolean validateExtensionMethod(Method method) {
        // marker annotation
        if (method.isAnnotationPresent(Extension.Method.class) == false) {
            return false;
        }

        // public static modifier
        int modifier = method.getModifiers();

        if (Modifier.isPublic(modifier) == false || Modifier.isStatic(modifier) == false) {
            return false;
        }

        // parameter type
        Class[] types = method.getParameterTypes();

        if (types.length == 0 || types[0].isPrimitive()) {
            return false;
        }

        return true; // This is valid extension method.
    }
}
