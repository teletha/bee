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

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import bee.Platform;
import bee.api.Command;
import bee.api.Scope;
import bee.api.Task;
import bee.util.PathSet;
import kiss.Events;
import kiss.I;
import kiss.Table;
import kiss.ThrowableFunction;
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
    @Command(value = "Enhance external class.", defaults = true)
    public Path enhance() {
        searchExtensionLibrary();
        return null;
    }

    /**
     * <p>
     * Search all extension libraries.
     * </p>
     */
    private void searchExtensionLibrary() {
        Table<String, ExtensionDefinition> map = Events.from(project.getDependency(Scope.Compile))
                .map(lib -> lib.getJar())
                .map(path -> FileSystems.newFileSystem(path, ClassLoader.getSystemClassLoader()).getPath("/"))
                .startWith(project.getSources().resolve("resources"))
                .map(path -> path.resolve("META-INF/services/" + EnhanceLibrary.class.getName()))
                .take(Files::exists)
                .flatIterable(Files::readAllLines)
                .map(ExtensionDefinition::of)
                .toTable(ExtensionDefinition::targetClass, ThrowableFunction.identity());

        System.out.println(map);
    }

    /**
     * @version 2016/12/11 1:30:03
     */
    @Value
    @Accessors(fluent = true)
    private static class ExtensionDefinition {

        String targetClass;

        String extensionClass;

        String extensionMethod;

        /**
         * @param line
         * @return
         */
        private static ExtensionDefinition of(String line) {
            String[] values = line.split(" ");

            return new ExtensionDefinition(values[0], values[1], values[2]);
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
