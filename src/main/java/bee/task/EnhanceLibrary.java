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

import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map.Entry;

import bee.Platform;
import bee.api.Command;
import bee.api.Scope;
import bee.api.Task;
import bee.util.PathSet;
import bee.util.Paths;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassWriter;
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
    @Command(value = "Enhance external class.", defaults = true)
    public Path enhance() {
        Path local = Paths.createDirectory(project.getOutput().resolve("enhance-library"));

        for (Entry<Path, List<ExtensionDefinition>> entry : searchExtensionLibrary().entrySet()) {
            // copy original archive
            Path original = entry.getKey();
            Path enhanced = local.resolve(original.getFileName());
            I.copy(original, enhanced);
            Path jar = jar(enhanced);

            Table<Class, ExtensionDefinition> table = Events.from(entry.getValue()).toTable(ExtensionDefinition::target);

            for (Entry<Class, List<ExtensionDefinition>> types : table.entrySet()) {
                Path classFile = jar.resolve(types.getKey().getName().replace('.', '/') + ".class");

                // start writing byte code
                ClassReader reader = new ClassReader(Files.newInputStream(classFile));
                ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                reader.accept(writer, 0);

                // retrieve byte code
                byte[] bytes = writer.toByteArray();

                // I.copy(new ByteArrayInputStream(bytes), Files.newOutputStream(classFile), true);
            }
        }
        return null;
    }

    // public static void main(String[] args) throws Throwable {
    // Map<String, String> env = new HashMap<>();
    // env.put("create", "true");
    // // locate file system by using the syntax
    // // defined in java.net.JarURLConnection
    // URI uri = URI.create("jar:file:/codeSamples/zipfs/zipfstest.zip");
    //
    // try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
    // Path externalTxtFile = Paths.get("/codeSamples/zipfs/SomeTextFile.txt");
    // Path pathInZipfile = zipfs.getPath("/SomeTextFile.txt");
    // // copy a file into the zip file
    // Files.copy(externalTxtFile, pathInZipfile, StandardCopyOption.REPLACE_EXISTING);
    // }
    // }

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

        String extensionClass;

        String extensionMethod;

        /**
         * @param line
         * @return
         * @throws ClassNotFoundException
         */
        static ExtensionDefinition of(String line) throws ClassNotFoundException {
            String[] values = line.split(" ");
            Class target = Class.forName(values[0]);
            Path archive = I.locate(target);

            return new ExtensionDefinition(target, archive == null ? Platform.JavaRuntime : archive, values[1], values[2]);
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
    public @interface ExtensionMethod {
    }
}
